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

import org.apache.pekko
import pekko.Done
import pekko.persistence.PersistentRepr
import pekko.persistence.jdbc.config.JournalConfig
import pekko.persistence.jdbc.db.SlickDatabase
import pekko.persistence.jdbc.journal.dao.legacy.{ ByteArrayJournalSerializer, JournalQueries }
import pekko.persistence.jdbc.migrator.MigratorSpec._
import pekko.persistence.query.EventEnvelope
import pekko.serialization.SerializationExtension
import pekko.stream.scaladsl.Sink

/**
 * The legacy rows are inserted with explicit ordering values that do not match their primary key or insertion order,
 * so the migration only keeps the original event order if it reads the legacy rows sorted by ordering.
 */
class H2JournalMigratorOrderingTest extends MigratorSpec("h2-application.conf") with MigratorSpec.H2Cleaner {

  it should "migrate the events in their original ordering" in {
    withActorSystem { implicit system =>
      val profile = SlickDatabase.profile(config, "slick")
      import profile.api._

      val journalConfig = new JournalConfig(system.settings.config.getConfig(JournalMigrator.JournalConfig))
      val legacyQueries = new JournalQueries(profile, journalConfig.journalTableConfiguration)
      val serializer = new ByteArrayJournalSerializer(
        SerializationExtension(system),
        journalConfig.pluginConfig.tagSeparator)

      def legacyRow(persistenceId: String, ordering: Long) =
        serializer
          .serialize(
            PersistentRepr(AccountCreated(1), 1L, persistenceId, writerUuid = "writer"),
            Set(MigratorSpec.Odd))
          .get
          .copy(ordering = ordering)

      // "a" sorts first by primary key and is inserted first, but it was written after "b"
      db.run(
        DBIO.seq(
          legacyQueries.JournalTable.forceInsert(legacyRow("a", 2L)),
          legacyQueries.JournalTable.forceInsert(legacyRow("b", 1L)))).futureValue

      val migrator = JournalMigrator(profile)
      try migrator.migrate().futureValue shouldBe Done
      finally migrator.close()

      withReadJournal { implicit readJournal =>
        readJournal
          .currentEventsByTag(MigratorSpec.Odd, offset = 0)
          .map((envelope: EventEnvelope) => envelope.persistenceId)
          .runWith(Sink.seq)
          .futureValue shouldBe Seq("b", "a")
      }
    }
  }
}
