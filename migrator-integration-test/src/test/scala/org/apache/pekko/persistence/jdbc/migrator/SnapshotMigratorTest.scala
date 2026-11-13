/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.migrator

import org.apache.pekko
import pekko.Done
import pekko.actor.ActorRef
import pekko.pattern.ask
import pekko.persistence.{ DeleteMessagesSuccess, Persistence, SelectedSnapshot, SnapshotSelectionCriteria }
import pekko.persistence.JournalProtocol.DeleteMessagesTo
import pekko.persistence.SnapshotProtocol.{ LoadSnapshot, LoadSnapshotResult }
import pekko.persistence.jdbc.db.SlickDatabase
import pekko.persistence.jdbc.migrator.MigratorSpec._

abstract class SnapshotMigratorTest(configName: String) extends MigratorSpec(configName) {

  it should "migrate snapshots" in {
    withLegacyActorSystem { implicit systemLegacy =>
      withReadJournal { implicit readJournal =>
        withTestActors() { (actorA1, actorA2, actorA3) =>
          (actorA1 ? CreateAccount(1)).futureValue
          (actorA2 ? CreateAccount(1)).futureValue
          (actorA3 ? CreateAccount(1)).futureValue
          for (_ <- 1 to 99) {
            (actorA1 ? Deposit(1)).futureValue
            (actorA2 ? Deposit(1)).futureValue
            (actorA3 ? Deposit(1)).futureValue
          }
          eventually {
            (actorA1 ? State).mapTo[Int].futureValue shouldBe 100
            (actorA2 ? State).mapTo[Int].futureValue shouldBe 100
            (actorA3 ? State).mapTo[Int].futureValue shouldBe 100
            countJournal().futureValue shouldBe 300
          }
        }
      }
    } // legacy persistence
    withActorSystem { implicit systemNew =>
      withReadJournal { implicit readJournal =>
        countJournal().futureValue shouldBe 0 // before migration
        // migrate exactly once: a retry would race with a migration that is still in flight
        val migrator = SnapshotMigrator(SlickDatabase.profile(config, "slick"))
        try migrator.migrateAll().futureValue shouldBe Done
        finally migrator.close()
        eventually {
          countJournal().futureValue shouldBe 0 // after migration
        }
        withTestActors() { (actorB1, actorB2, actorB3) =>
          eventually {
            (actorB1 ? State).mapTo[Int].futureValue shouldBe 100
            (actorB2 ? State).mapTo[Int].futureValue shouldBe 100
            (actorB3 ? State).mapTo[Int].futureValue shouldBe 100
          }
        }
      }
    } // new persistence
  }
  it should "migrate the latest snapshot of an actor that deleted its events" in {
    val persistenceId = "test-account-1"
    withLegacyActorSystem { implicit systemLegacy =>
      withTestActors() { (actorA1, _, _) =>
        (actorA1 ? CreateAccount(1)).futureValue
        for (_ <- 1 to 9) (actorA1 ? Deposit(1)).futureValue
        // the balance of 10 makes the actor save a snapshot at sequence number 10
        eventually {
          (Persistence(systemLegacy).snapshotStoreFor(null) ? LoadSnapshot(
            persistenceId,
            SnapshotSelectionCriteria.Latest,
            Long.MaxValue)).mapTo[LoadSnapshotResult].futureValue.snapshot.map(_.metadata.sequenceNr) shouldBe Some(
            10L)
        }
        // the legacy DAO keeps only the highest journal row and marks it deleted, so the actor has no undeleted events
        pekko.pattern.extended
          .ask(Persistence(systemLegacy).journalFor(null), (ref: ActorRef) => DeleteMessagesTo(persistenceId, 10L, ref))
          .futureValue shouldBe a[DeleteMessagesSuccess]
      }
    } // legacy persistence
    withActorSystem { implicit systemNew =>
      val migrator = SnapshotMigrator(SlickDatabase.profile(config, "slick"))
      try migrator.migrateLatest().futureValue shouldBe Done
      finally migrator.close()
      eventually {
        (Persistence(systemNew).snapshotStoreFor(null) ? LoadSnapshot(
          persistenceId,
          SnapshotSelectionCriteria.Latest,
          Long.MaxValue)).mapTo[LoadSnapshotResult].futureValue.snapshot.collect {
          case SelectedSnapshot(metadata, snapshot) => (metadata.sequenceNr, snapshot)
        } shouldBe Some((10L, 10))
      }
    } // new persistence
  }
}

class H2SnapshotMigratorTest extends SnapshotMigratorTest("h2-application.conf") with MigratorSpec.H2Cleaner
