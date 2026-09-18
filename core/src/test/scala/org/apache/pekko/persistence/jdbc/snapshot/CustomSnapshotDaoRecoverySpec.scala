/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.persistence.jdbc.snapshot

import org.apache.pekko
import pekko.actor.{ ActorRef, ActorSystem }
import pekko.actor.typed.Behavior
import pekko.actor.typed.scaladsl.adapter._
import pekko.persistence.{
  DeleteSnapshotsSuccess,
  Persistence,
  SnapshotMetadata,
  SnapshotSelectionCriteria => ClassicSnapshotSelectionCriteria
}
import pekko.persistence.SnapshotProtocol.DeleteSnapshots
import pekko.persistence.jdbc.config.{ SlickConfiguration, SnapshotConfig }
import pekko.persistence.jdbc.db.SlickDatabase
import pekko.persistence.jdbc.snapshot.dao.{ DefaultSnapshotDao, SnapshotDao }
import pekko.persistence.jdbc.testkit.internal.H2
import pekko.persistence.jdbc.util.DropCreate
import pekko.persistence.typed.{
  DeleteSnapshotsCompleted,
  DeletionTarget,
  EventSourcedSignal,
  PersistenceId,
  RecoveryCompleted,
  SnapshotAdapter,
  SnapshotCompleted,
  SnapshotSelectionCriteria
}
import pekko.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior, Recovery, RetentionCriteria }
import pekko.serialization.{ Serialization, SerializationExtension }
import pekko.stream.{ Materializer, SystemMaterializer }
import pekko.testkit.{ TestKit, TestProbe }
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import slick.jdbc.{ JdbcBackend, JdbcProfile }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

// Implements only the pre-criteria API, as an existing custom DAO would.
class UpperBoundSnapshotDao(
    db: JdbcBackend#Database,
    profile: JdbcProfile,
    config: SnapshotConfig,
    serialization: Serialization)(implicit ec: ExecutionContext, mat: Materializer)
    extends SnapshotDao {
  private val delegate = new DefaultSnapshotDao(db, profile, config, serialization)

  override def latestSnapshot(pid: String) = delegate.latestSnapshot(pid)
  override def snapshotForMaxTimestamp(pid: String, timestamp: Long) =
    delegate.snapshotForMaxTimestamp(pid, timestamp)
  override def snapshotForMaxSequenceNr(pid: String, sequenceNr: Long) =
    delegate.snapshotForMaxSequenceNr(pid, sequenceNr)
  override def snapshotForMaxSequenceNrAndMaxTimestamp(pid: String, sequenceNr: Long, timestamp: Long) =
    delegate.snapshotForMaxSequenceNrAndMaxTimestamp(pid, sequenceNr, timestamp)
  override def deleteAllSnapshots(pid: String) = delegate.deleteAllSnapshots(pid)
  override def deleteUpToMaxSequenceNr(pid: String, sequenceNr: Long) =
    delegate.deleteUpToMaxSequenceNr(pid, sequenceNr)
  override def deleteUpToMaxTimestamp(pid: String, timestamp: Long) =
    delegate.deleteUpToMaxTimestamp(pid, timestamp)
  override def deleteUpToMaxSequenceNrAndMaxTimestamp(pid: String, sequenceNr: Long, timestamp: Long) =
    delegate.deleteUpToMaxSequenceNrAndMaxTimestamp(pid, sequenceNr, timestamp)
  override def delete(pid: String, sequenceNr: Long) = delegate.delete(pid, sequenceNr)
  override def save(metadata: SnapshotMetadata, snapshot: Any) = delegate.save(metadata, snapshot)
}

object CustomSnapshotDaoRecoverySpec {
  val config = ConfigFactory.parseString(s"""
    jdbc-journal.dao = "org.apache.pekko.persistence.jdbc.journal.dao.DefaultJournalDao"
    jdbc-journal.slick = $${slick}
    jdbc-snapshot-store.dao = "${classOf[UpperBoundSnapshotDao].getName}"
    jdbc-snapshot-store.slick = $${slick}
    slick.db.url = "jdbc:h2:mem:custom-snapshot-dao;DATABASE_TO_UPPER=false;"
  """).withFallback(ConfigFactory.load("h2-application.conf")).resolve()

  sealed trait Command
  case object Append extends Command
  case object GetState extends Command
  case object Stop extends Command

  def behavior(
      pid: String,
      replies: ActorRef,
      signals: ActorRef,
      recoveredSnapshots: ActorRef,
      criteria: SnapshotSelectionCriteria,
      retention: Boolean): Behavior[Command] = {
    val persistent = EventSourcedBehavior[Command, Int, Int](
      PersistenceId.ofUniqueId(pid),
      0,
      (_, command) =>
        command match {
          case Append   => Effect.persist(1).thenRun(state => replies ! state)
          case GetState => Effect.none.thenRun(state => replies ! state)
          case Stop     => Effect.stop()
        },
      (state, event) => state + event)
      .withRecovery(Recovery.withSnapshotSelectionCriteria(criteria))
      .snapshotAdapter(new SnapshotAdapter[Int] {
        override def toJournal(state: Int): Any = state
        override def fromJournal(snapshot: Any): Int = {
          recoveredSnapshots ! snapshot
          snapshot.asInstanceOf[Int]
        }
      })
      .receiveSignal { case (_, signal: EventSourcedSignal) => signals ! signal }

    if (retention) persistent.withRetention(RetentionCriteria.snapshotEvery(2, 1))
    else persistent.snapshotWhen((_, _, sequenceNr) => sequenceNr == 2)
  }
}

class CustomSnapshotDaoRecoverySpec
    extends TestKit(ActorSystem("CustomSnapshotDaoRecoverySpec", CustomSnapshotDaoRecoverySpec.config))
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with DropCreate {
  import CustomSnapshotDaoRecoverySpec._

  override val config = CustomSnapshotDaoRecoverySpec.config
  override lazy val db = SlickDatabase.database(config, new SlickConfiguration(config.getConfig("slick")), "slick.db")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = SystemMaterializer(system).materializer
  implicit val pc: PatienceConfig = PatienceConfig(timeout = 10.seconds)

  private lazy val snapshots = new DefaultSnapshotDao(
    db,
    SlickDatabase.profile(config, "slick"),
    new SnapshotConfig(system.settings.config.getConfig("jdbc-snapshot-store")),
    SerializationExtension(system))

  override def beforeAll(): Unit = {
    super.beforeAll()
    dropAndCreate(H2)
  }

  override def afterAll(): Unit = {
    try TestKit.shutdownActorSystem(system)
    finally {
      db.close()
      super.afterAll()
    }
  }

  "An existing custom snapshot DAO" should {
    "continue typed snapshot retention" in {
      val pid = "custom-retention"
      val replies = TestProbe()
      val signals = TestProbe()
      val recovered = TestProbe()
      val actor = system.spawn(behavior(pid, replies.ref, signals.ref, recovered.ref,
        SnapshotSelectionCriteria.latest, retention = true), pid)
      signals.expectMsg(RecoveryCompleted)

      (1 to 6).foreach { sequenceNr =>
        actor ! Append
        replies.expectMsg(sequenceNr)
        if (sequenceNr % 2 == 0) {
          signals.expectMsgType[SnapshotCompleted].metadata.sequenceNr shouldBe sequenceNr.toLong
          if (sequenceNr >= 4) {
            val deleted = signals.expectMsgType[DeleteSnapshotsCompleted]
            deleted.target.asInstanceOf[DeletionTarget.Criteria].selection.maxSequenceNr shouldBe sequenceNr - 2L
          }
        }
      }

      snapshots.snapshotForMaxSequenceNr(pid, 4).futureValue shouldBe None
      snapshots.latestSnapshot(pid).futureValue.map(_._1.sequenceNr) shouldBe Some(6L)
      val stopped = TestProbe()
      stopped.watch(actor.toClassic)
      actor ! Stop
      stopped.expectTerminated(actor.toClassic)
    }

    "preserve upper-bound deletion for a nonzero retention minimum" in {
      val pid = "custom-bounded-delete"
      Seq(1L, 2L, 4L, 6L).foreach { sequenceNr =>
        snapshots.save(SnapshotMetadata(pid, sequenceNr, sequenceNr * 100), sequenceNr.toInt).futureValue
      }
      val other = SnapshotMetadata("custom-bounded-delete-other", 1L, 100L)
      snapshots.save(other, 1).futureValue
      val criteria = ClassicSnapshotSelectionCriteria(maxSequenceNr = 4, minSequenceNr = 2)
      val reply = TestProbe()
      Persistence(system).snapshotStoreFor("jdbc-snapshot-store").tell(DeleteSnapshots(pid, criteria), reply.ref)
      reply.expectMsg(DeleteSnapshotsSuccess(criteria))
      // Old custom DAOs deliberately retain their upper-bound-only deletion behavior.
      snapshots.snapshotForMaxSequenceNr(pid, 4).futureValue shouldBe None
      snapshots.latestSnapshot(pid).futureValue.map(_._1.sequenceNr) shouldBe Some(6L)
      snapshots.latestSnapshot(other.persistenceId).futureValue.map(_._1) shouldBe Some(other)
    }

    Seq(("sequence", true, false), ("timestamp", false, true), ("both", true, true)).foreach {
      case (label, minSequence, minTimestamp) =>
        s"recover from an older snapshot and replay events with a $label minimum" in {
          val pid = s"custom-recovery-$label"
          val replies = TestProbe()
          val signals = TestProbe()
          val recovered = TestProbe()
          val initial = system.spawn(behavior(pid, replies.ref, signals.ref, recovered.ref,
            SnapshotSelectionCriteria.latest, retention = false), s"$pid-initial")
          signals.expectMsg(RecoveryCompleted)
          (1 to 4).foreach { sequenceNr =>
            initial ! Append
            replies.expectMsg(sequenceNr)
            if (sequenceNr == 2) signals.expectMsgType[SnapshotCompleted]
          }
          val metadata = snapshots.latestSnapshot(pid).futureValue.get._1
          metadata.sequenceNr shouldBe 2L
          val stopped = TestProbe()
          stopped.watch(initial.toClassic)
          initial ! Stop
          stopped.expectTerminated(initial.toClassic)

          val criteria = SnapshotSelectionCriteria.latest
            .withMinSequenceNr(if (minSequence) 3L else 0L)
            .withMinTimestamp(if (minTimestamp) metadata.timestamp + 1 else 0L)
          val restarted = system.spawn(behavior(pid, replies.ref, signals.ref, recovered.ref,
            criteria, retention = false), s"$pid-restarted")
          signals.expectMsg(RecoveryCompleted)
          recovered.expectMsg(2)
          restarted ! GetState
          replies.expectMsg(4)
          stopped.watch(restarted.toClassic)
          restarted ! Stop
          stopped.expectTerminated(restarted.toClassic)
        }
    }
  }
}
