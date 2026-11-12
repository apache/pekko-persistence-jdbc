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

package org.apache.pekko.persistence.jdbc.query.dao

import com.typesafe.config.ConfigFactory
import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.persistence.PersistentRepr
import pekko.persistence.jdbc.SimpleSpec
import pekko.persistence.jdbc.config.ReadJournalConfig
import pekko.persistence.jdbc.query.dao.legacy.ByteArrayReadJournalDao
import pekko.serialization.SerializationExtension
import pekko.stream.SystemMaterializer
import pekko.stream.scaladsl.Sink
import slick.jdbc.{ H2Profile, JdbcBackend, OracleProfile, SetParameter }

import scala.concurrent.duration._

class OracleReadJournalDaoTest extends SimpleSpec {
  implicit val pc: PatienceConfig = PatienceConfig(timeout = 10.seconds)

  "The legacy Oracle events-by-tag query" should "filter numeric deleted flags and respect tag and offset bounds" in {
    implicit val system: ActorSystem =
      ActorSystem("OracleReadJournalDaoTest", ConfigFactory.load("h2-application.conf"))
    implicit val ec = system.dispatcher
    implicit val mat = SystemMaterializer(system).materializer
    // Execute the actual Oracle DAO's plain SQL using H2's Oracle compatibility mode.
    // This is not a substitute for running the Oracle integration suites.
    val db = JdbcBackend.Database.forURL(
      "jdbc:h2:mem:oracle-read-journal;MODE=Oracle;DATABASE_TO_UPPER=false",
      driver = "org.h2.Driver",
      keepAliveConnection = true)
    try {
      import H2Profile.api._
      db.run(sqlu"""CREATE TABLE "journal" (
        "ordering" BIGINT, "deleted" NUMBER(1) CHECK ("deleted" IN (0, 1)),
        "persistence_id" VARCHAR(255), "sequence_number" BIGINT, "message" BLOB, "tags" VARCHAR(255))""").futureValue
      val readConfig = new ReadJournalConfig(system.settings.config.getConfig("jdbc-read-journal"))
      val dao = new ByteArrayReadJournalDao(db, OracleProfile, readConfig, SerializationExtension(system))
      implicit val bytesSetter: SetParameter[Array[Byte]] = SetParameter((bytes, params) => params.setBytes(bytes))
      def insert(ordering: Long, deleted: Int, tag: String): Unit = {
        val repr = PersistentRepr(s"event-$ordering", sequenceNr = ordering, persistenceId = "pid")
        val bytes = dao.serializer.serialize(repr, Set(tag)).get.message
        db.run(sqlu"""INSERT INTO "journal" VALUES ($ordering, $deleted, 'pid', $ordering, $bytes, $tag)""").futureValue
      }
      insert(1, 0, "wanted")
      insert(2, 1, "wanted")
      insert(3, 0, "wanted")
      insert(4, 0, "unwanted")
      insert(5, 0, "wanted")
      val result = dao.eventsByTag("wanted", 1, 4, 10).runWith(Sink.seq).futureValue
      result.map(_.get._3) shouldBe Seq(3L)
      result.map(_.get._1.payload) shouldBe Seq("event-3")
      dao.eventsByTag("wanted", -1, 5, 1).runWith(Sink.seq).futureValue.map(_.get._3) shouldBe Seq(1L)
    } finally {
      db.close()
      system.terminate().futureValue
    }
  }
}
