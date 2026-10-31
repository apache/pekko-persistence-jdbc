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

import scala.concurrent.duration._

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.persistence.jdbc.SimpleSpec
import pekko.persistence.jdbc.db.SlickDatabase
import pekko.persistence.jdbc.util.ConnectionPools
import com.typesafe.config.{ Config, ConfigFactory }

class MigratorCloseTest extends SimpleSpec {

  implicit val pc: PatienceConfig = PatienceConfig(timeout = 1.minute)

  private val config: Config = ConfigFactory.load("h2-application.conf")
  private val profile = SlickDatabase.profile(config, "slick")

  private def withActorSystem(f: ActorSystem => Unit): Unit = {
    val system = ActorSystem("migrator-close-test", config)
    try f(system)
    finally system.terminate().futureValue
  }

  it should "close the database that the journal migrator opened" in withActorSystem { implicit system =>
    val migrator = JournalMigrator(profile)
    ConnectionPools.isClosed(migrator.journalDB) shouldBe false

    migrator.close()

    ConnectionPools.isClosed(migrator.journalDB) shouldBe true
  }

  it should "close the databases that the snapshot migrator opened" in withActorSystem { implicit system =>
    val migrator = SnapshotMigrator(profile)
    ConnectionPools.isClosed(migrator.snapshotDB) shouldBe false
    ConnectionPools.isClosed(migrator.journalDB) shouldBe false

    migrator.close()

    ConnectionPools.isClosed(migrator.snapshotDB) shouldBe true
    ConnectionPools.isClosed(migrator.journalDB) shouldBe true
  }
}
