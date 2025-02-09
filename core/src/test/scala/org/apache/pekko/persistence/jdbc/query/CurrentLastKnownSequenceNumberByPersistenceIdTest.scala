/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.query

import org.scalatest.concurrent.ScalaFutures

abstract class CurrentLastKnownSequenceNumberByPersistenceIdTest(
    config: String
) extends QueryTestSpec(config) with ScalaFutures {

  it should "return None for unknown persistenceId" in withActorSystem { implicit system =>
    val journalOps = new ScalaJdbcReadJournalOperations(system)

    journalOps
      .currentLastKnownSequenceNumberByPersistenceId("unknown")
      .futureValue shouldBe None
  }

  it should "return last sequence number for known persistenceId" in withActorSystem { implicit system =>
    val journalOps = new ScalaJdbcReadJournalOperations(system)

    withTestActors() { (actor1, _, _) =>
      actor1 ! 1
      actor1 ! 2
      actor1 ! 3
      actor1 ! 4

      eventually {
        journalOps
          .currentLastKnownSequenceNumberByPersistenceId("my-1")
          .futureValue shouldBe Some(4)

        // Just ensuring that query targets the correct persistenceId.
        journalOps
          .currentLastKnownSequenceNumberByPersistenceId("my-2")
          .futureValue shouldBe None
      }
    }
  }
}

class H2ScalaCurrentLastKnownSequenceNumberByPersistenceIdTest
    extends CurrentLastKnownSequenceNumberByPersistenceIdTest("h2-shared-db-application.conf")
    with H2Cleaner
