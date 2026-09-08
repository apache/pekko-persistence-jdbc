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

package org.apache.pekko.persistence.jdbc.migrator

import org.apache.pekko.actor.ActorSystem

/**
 * Checks the actor system lifecycle contract of the [[MigratorSpec]] fixtures. A failing test body must not leak a
 * running actor system, because the migration stream it still owns keeps writing into the journal tables that the next
 * test recreates in its `beforeEach`.
 *
 * No database is required: the fixtures are exercised without starting any persistent actor.
 */
class MigratorSpecFixtureTest extends MigratorSpec("h2-application.conf") {

  private def assertTerminatesWhenBodyFails(fixture: (ActorSystem => Unit) => Unit): Unit = {
    var captured: Option[ActorSystem] = None
    val thrown = intercept[RuntimeException] {
      fixture { system =>
        captured = Some(system)
        throw new RuntimeException("boom")
      }
    }
    thrown.getMessage shouldBe "boom"
    val system = captured.getOrElse(fail("the fixture never ran the test body"))
    system.whenTerminated.futureValue
  }

  it should "terminate the actor system when the test body fails" in {
    assertTerminatesWhenBodyFails(withActorSystem)
  }

  it should "terminate the legacy actor system when the test body fails" in {
    assertTerminatesWhenBodyFails(withLegacyActorSystem)
  }
}
