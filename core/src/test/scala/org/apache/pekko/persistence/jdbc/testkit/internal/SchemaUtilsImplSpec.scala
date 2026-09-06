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

package org.apache.pekko.persistence.jdbc.testkit.internal

import scala.concurrent.duration._

import org.apache.pekko
import pekko.Done
import pekko.actor.ActorSystem
import pekko.persistence.jdbc.db.SlickExtension
import pekko.persistence.jdbc.util.{ ConnectionPools, RecordingSlickDatabaseProvider }
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.LoggerFactory

class SchemaUtilsImplSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 1.minute)

  private val logger = LoggerFactory.getLogger(classOf[SchemaUtilsImplSpec])

  private def withActorSystem(config: Config)(f: ActorSystem => Unit): Unit = {
    val system = ActorSystem("SchemaUtilsImplSpec", config)
    try f(system)
    finally system.terminate().futureValue
  }

  "SchemaUtilsImpl" must {
    "close the database that was created to apply the schema script when the actor system terminates" in {
      RecordingSlickDatabaseProvider.clear()
      val config = ConfigFactory
        .parseString("""pekko-persistence-jdbc.database-provider-fqcn =
            |  "org.apache.pekko.persistence.jdbc.util.RecordingSlickDatabaseProvider"""".stripMargin)
        .withFallback(ConfigFactory.load("h2-application.conf"))

      val system = ActorSystem("SchemaUtilsImplSpec", config)
      val database =
        try {
          SchemaUtilsImpl.createIfNotExists("jdbc-journal", logger)(system).futureValue shouldBe Done

          val databases = RecordingSlickDatabaseProvider.databases
          databases should have size 1
          // the database has to stay open, closing it would drop an H2 in-memory schema right after creating it
          ConnectionPools.isClosed(databases.head) shouldBe false
          databases.head
        } finally system.terminate().futureValue

      ConnectionPools.isClosed(database) shouldBe true
    }

    "leave a shared database open after applying the schema script" in {
      withActorSystem(ConfigFactory.load("h2-shared-db-application.conf")) { implicit system =>
        SchemaUtilsImpl.createIfNotExists("jdbc-journal", logger).futureValue shouldBe Done

        val sharedDb = SlickExtension(system).database(system.settings.config.getConfig("jdbc-journal"))
        sharedDb.allowShutdown shouldBe false
        ConnectionPools.isClosed(sharedDb.database) shouldBe false
      }
    }
  }
}
