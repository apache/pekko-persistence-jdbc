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

package org.apache.pekko.persistence.jdbc.query

import org.scalatest.concurrent.ScalaFutures

abstract class CurrentLastKnownSequenceNumberByPersistenceIdTest(config: String) extends QueryTestSpec(config)
    with ScalaFutures {

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
