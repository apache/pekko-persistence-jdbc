/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.migrator

import org.apache.pekko
import pekko.Done
import pekko.pattern.ask
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
        eventually {
          countJournal().futureValue shouldBe 0 // before migration
          SnapshotMigrator(SlickDatabase.profile(config, "slick")).migrateAll().futureValue shouldBe Done
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
}

class H2SnapshotMigratorTest extends SnapshotMigratorTest("h2-application.conf") with MigratorSpec.H2Cleaner
