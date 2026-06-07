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

package org.apache.pekko.persistence.jdbc.journal.dao

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ActorSystem
import pekko.persistence.jdbc.query.{ H2Cleaner, QueryTestSpec }
import pekko.persistence.{ AtomicWrite, PersistentRepr }
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.stream.{ Materializer, SystemMaterializer }

import java.io.NotSerializableException
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Covers the database-coupled side of the batched message stream: hard deletes as a
 * representative contract check of the `messages` query (ascending order, limit, inclusive
 * range), soft deletes (the `deleted` flag filter), the journal's own prefix purge
 * (`delete`), and serialization failures. The full gap state-machine specification lives
 * in MessagesWithBatchTest.
 */
abstract class MessagesWithBatchDatabaseContractTest(configFile: String) extends QueryTestSpec(configFile) {

  private lazy val journalQueries =
    new JournalQueries(profile, journalConfig.eventJournalTableConfiguration, journalConfig.eventTagTableConfiguration)

  it should "emit all messages when a hard-deleted gap is wider than the batch size" in
  withActorSystem { implicit system =>
    withSetup { (dao, persistenceId) =>
      persistMessages(dao, persistenceId, count = 4)
      hardDelete(persistenceId, 2, 3)

      val emitted = emittedSequenceNrs(dao, persistenceId, toSequenceNr = 4, batchSize = 1)

      emitted shouldBe Seq(1L, 4L)
    }
  }

  it should "emit messages beyond a hard-deleted gap wider than the batch size when polling with a refresh interval" in
  withActorSystem { implicit system =>
    withSetup { (dao, persistenceId) =>
      persistMessages(dao, persistenceId, count = 4)
      hardDelete(persistenceId, 2, 3)

      val emitted =
        emittedSequenceNrs(dao, persistenceId, toSequenceNr = 4, batchSize = 1, count = Some(2),
          refreshInterval = Some(50.millis))

      emitted shouldBe Seq(1L, 4L)
    }
  }

  it should "emit all messages when a gap mixes hard-deleted and soft-deleted messages" in
  withActorSystem { implicit system =>
    withSetup { (dao, persistenceId) =>
      persistMessages(dao, persistenceId, count = 5)
      hardDelete(persistenceId, 2)
      softDelete(persistenceId, 3)
      hardDelete(persistenceId, 4)

      val emitted = emittedSequenceNrs(dao, persistenceId, toSequenceNr = 5, batchSize = 1)

      emitted shouldBe Seq(1L, 5L)
    }
  }

  it should "complete with no messages when every message is soft-deleted" in
  withActorSystem { implicit system =>
    withSetup { (dao, persistenceId) =>
      persistMessages(dao, persistenceId, count = 3)
      softDelete(persistenceId, 1, 2, 3)

      val emitted = emittedSequenceNrs(dao, persistenceId, toSequenceNr = 3, batchSize = 1)

      emitted shouldBe empty
    }
  }

  // The journal's prefix purge hard-deletes the messages below the highest affected sequence
  // number and soft-deletes that one, leaving a leading gap: the scenario behind issue #516
  it should "emit the remaining messages after a prefix purge through the journal's delete" in
  withActorSystem { implicit system =>
    withSetup { (dao, persistenceId) =>
      persistMessages(dao, persistenceId, count = 4)
      dao.delete(persistenceId, 3).futureValue

      val emitted = emittedSequenceNrs(dao, persistenceId, toSequenceNr = 4, batchSize = 1)

      emitted shouldBe Seq(4L)
    }
  }

  it should "fail the stream when the last message in a batch cannot be deserialized" in
  withActorSystem { implicit system =>
    withSetup { (dao, persistenceId) =>
      persistMessages(dao, persistenceId, count = 1)
      persistCorruptMessage(persistenceId, sequenceNr = 2)

      val result = dao
        .messagesWithBatch(persistenceId, fromSequenceNr = 1, toSequenceNr = 2, batchSize = 2,
          refreshInterval = None)
        .runWith(Sink.seq)

      result.failed.futureValue shouldBe a[NotSerializableException]
    }
  }

  private implicit def materializer(implicit system: ActorSystem): Materializer =
    SystemMaterializer(system).materializer

  private def withSetup(f: (JournalDao, String) => Unit)(implicit system: ActorSystem): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher
    withDao(dao => f(dao, UUID.randomUUID().toString))
  }

  private def persistMessages(dao: JournalDao, persistenceId: String, count: Int): Unit = {
    val writerUuid = UUID.randomUUID().toString
    val payload = Array.fill(8)('a'.toByte)
    val writes = (1 to count).map { sequenceNr =>
      AtomicWrite(immutable.Seq(PersistentRepr(payload, sequenceNr, persistenceId, writerUuid = writerUuid)))
    }
    dao.asyncWriteMessages(writes).futureValue
  }

  private def hardDelete(persistenceId: String, sequenceNrs: Long*): Unit = {
    import profile.api._
    val deleteRows = journalQueries.JournalTable
      .filter(row => row.persistenceId === persistenceId && row.sequenceNumber.inSet(sequenceNrs))
      .delete
    db.run(deleteRows).futureValue
  }

  private def softDelete(persistenceId: String, sequenceNrs: Long*): Unit =
    sequenceNrs.foreach { sequenceNr =>
      db.run(journalQueries.markSeqNrJournalMessagesAsDeleted(persistenceId, sequenceNr)).futureValue
    }

  /** Inserts a journal row whose serializer id is unknown, so reading it yields a Failure. */
  private def persistCorruptMessage(persistenceId: String, sequenceNr: Long): Unit = {
    import profile.api._
    val unknownSerializerId = 999999
    val corruptRow = JournalTables.JournalPekkoSerializationRow(Long.MinValue, deleted = false, persistenceId,
      sequenceNr, writer = UUID.randomUUID().toString, writeTimestamp = 0L, adapterManifest = "",
      eventPayload = Array.fill(8)('x'.toByte),
      eventSerId = unknownSerializerId, eventSerManifest = "", metaPayload = None, metaSerId = None,
      metaSerManifest = None)
    db.run(journalQueries.JournalTable += corruptRow).futureValue
  }

  /**
   * Collects the sequence numbers the batched message stream emits. A polling stream never
   * completes on its own, so pass `count` to bound it.
   */
  private def emittedSequenceNrs(dao: JournalDao, persistenceId: String, toSequenceNr: Long, batchSize: Int,
      refreshInterval: Option[FiniteDuration] = None, count: Option[Int] = None)(
      implicit system: ActorSystem): Seq[Long] = {
    val sequenceNrs = sequenceNrSource(dao, persistenceId, toSequenceNr, batchSize, refreshInterval)
    count
      .fold(sequenceNrs)(sequenceNrs.take(_))
      .completionTimeout(10.seconds)
      .runWith(Sink.seq)
      .futureValue
  }

  private def sequenceNrSource(dao: JournalDao, persistenceId: String, toSequenceNr: Long,
      batchSize: Int, refreshInterval: Option[FiniteDuration])(implicit system: ActorSystem): Source[Long, NotUsed] =
    dao
      .messagesWithBatch(persistenceId, fromSequenceNr = 1, toSequenceNr, batchSize,
        refreshInterval.map(_ -> system.scheduler))
      .mapAsync(1)(Future.fromTry)
      .map { case (repr, _) => repr.sequenceNr }
}

final class H2MessagesWithBatchDatabaseContractTest
    extends MessagesWithBatchDatabaseContractTest("h2-application.conf")
    with H2Cleaner
