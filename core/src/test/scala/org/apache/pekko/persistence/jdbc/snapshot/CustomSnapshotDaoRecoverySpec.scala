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

import scala.concurrent.{ ExecutionContext, Future }

import org.apache.pekko
import pekko.persistence.{
  DeleteSnapshotsSuccess,
  Persistence,
  SelectedSnapshot,
  SnapshotMetadata,
  SnapshotSelectionCriteria
}
import pekko.persistence.SnapshotProtocol.{ DeleteSnapshots, LoadSnapshot, LoadSnapshotResult }
import pekko.persistence.jdbc.SharedActorSystemTestSpec
import pekko.persistence.jdbc.config.SnapshotConfig
import pekko.persistence.jdbc.db.SlickDatabase
import pekko.persistence.jdbc.snapshot.dao.{ DefaultSnapshotDao, SnapshotDao }
import pekko.persistence.jdbc.testkit.internal.H2
import pekko.serialization.Serialization
import pekko.stream.Materializer
import pekko.testkit.TestProbe
import com.typesafe.config.ConfigValueFactory
import slick.jdbc.{ JdbcBackend, JdbcProfile }

// Models an existing custom DAO that implements only the pre-criteria API.
class UpperBoundSnapshotDao(
    db: JdbcBackend#Database,
    profile: JdbcProfile,
    config: SnapshotConfig,
    serialization: Serialization)(implicit ec: ExecutionContext, mat: Materializer)
    extends DefaultSnapshotDao(db, profile, config, serialization)
    with SnapshotDao {
  override def snapshotForCriteria(
      persistenceId: String,
      criteria: SnapshotSelectionCriteria): Future[Option[(SnapshotMetadata, Any)]] =
    super[SnapshotDao].snapshotForCriteria(persistenceId, criteria)

  override def deleteByCriteria(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Unit] =
    super[SnapshotDao].deleteByCriteria(persistenceId, criteria)
}

class CustomSnapshotDaoRecoverySpec
    extends SharedActorSystemTestSpec(
      "h2-application.conf",
      Map("jdbc-snapshot-store.dao" -> ConfigValueFactory.fromAnyRef(classOf[UpperBoundSnapshotDao].getName))) {

  private lazy val snapshots = new DefaultSnapshotDao(
    db,
    SlickDatabase.profile(config, "slick"),
    new SnapshotConfig(system.settings.config.getConfig("jdbc-snapshot-store")),
    serialization)
  private lazy val snapshotStore = Persistence(system).snapshotStoreFor("jdbc-snapshot-store")

  override def beforeAll(): Unit = {
    dropAndCreate(H2)
    super.beforeAll()
  }

  it should "filter an upper-bound custom DAO result against lower bounds" in {
    val persistenceId = "custom-bounded-load"
    val older = SnapshotMetadata(persistenceId, 2L, 200L)
    snapshots.save(older, "older").futureValue

    val criteria = SnapshotSelectionCriteria(minSequenceNr = 3L, minTimestamp = 201L)
    val probe = TestProbe()
    snapshotStore.tell(LoadSnapshot(persistenceId, criteria, Long.MaxValue), probe.ref)
    probe.expectMsg(LoadSnapshotResult(None, Long.MaxValue))

    val matching = SnapshotMetadata(persistenceId, 4L, 400L)
    snapshots.save(matching, "matching").futureValue
    snapshotStore.tell(LoadSnapshot(persistenceId, criteria, Long.MaxValue), probe.ref)
    probe.expectMsg(LoadSnapshotResult(Some(SelectedSnapshot(matching, "matching")), Long.MaxValue))
  }

  it should "preserve upper-bound deletion for a custom DAO" in {
    val persistenceId = "custom-bounded-delete"
    Seq(1L, 2L, 4L, 6L).foldLeft(Future.successful(())) { (saved, sequenceNr) =>
      saved.flatMap(_ =>
        snapshots.save(SnapshotMetadata(persistenceId, sequenceNr, sequenceNr * 100), sequenceNr.toInt))
    }.futureValue

    val criteria = SnapshotSelectionCriteria(maxSequenceNr = 4L, minSequenceNr = 2L)
    val probe = TestProbe()
    snapshotStore.tell(DeleteSnapshots(persistenceId, criteria), probe.ref)
    probe.expectMsg(DeleteSnapshotsSuccess(criteria))

    snapshots.snapshotForMaxSequenceNr(persistenceId, 4L).futureValue shouldBe None
    snapshots.latestSnapshot(persistenceId).futureValue.map(_._1.sequenceNr) shouldBe Some(6L)
  }
}
