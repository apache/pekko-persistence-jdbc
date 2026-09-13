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

package org.apache.pekko.persistence.jdbc.journal

import java.util.concurrent.CopyOnWriteArrayList

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

import org.apache.pekko
import pekko.persistence.Persistence
import pekko.persistence.jdbc.SingleActorSystemPerTestSpec
import pekko.persistence.jdbc.config.JournalConfig
import pekko.persistence.jdbc.journal.dao.DefaultJournalDao
import pekko.serialization.Serialization
import pekko.stream.Materializer
import com.typesafe.config.ConfigValueFactory
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

/**
 * A journal dao that publishes the instances that the journal plugin creates, so that a test can look at the state of
 * the dao after the plugin actor has stopped.
 */
class RecordingJournalDao(
    db: Database,
    profile: JdbcProfile,
    journalConfig: JournalConfig,
    serialization: Serialization)(implicit ec: ExecutionContext, mat: Materializer)
    extends DefaultJournalDao(db, profile, journalConfig, serialization) {
  RecordingJournalDao.instances.add(this)
}

object RecordingJournalDao {
  val instances = new CopyOnWriteArrayList[RecordingJournalDao]

  def clear(): Unit = instances.clear()
}

class JdbcAsyncWriteJournalShutdownTest
    extends SingleActorSystemPerTestSpec(
      "h2-shared-db-application.conf",
      Map("jdbc-journal.dao" -> ConfigValueFactory.fromAnyRef(classOf[RecordingJournalDao].getName))) {

  it should "complete the write queue of its dao when the journal actor stops" in {
    RecordingJournalDao.clear()
    withActorSystem { implicit system =>
      val journal = Persistence(system).journalFor("jdbc-journal")
      // the plugin actor, and with it the dao, is created asynchronously
      val dao = eventually {
        val daos = RecordingJournalDao.instances.asScala.toList
        daos should have size 1
        daos.head
      }

      killActors(journal)

      val failure = dao.queueWriteJournalRows(Seq.empty).failed.futureValue
      failure.getMessage should include("the queue was closed")
    }
  }
}
