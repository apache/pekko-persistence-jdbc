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

package org.apache.pekko.persistence.jdbc.journal.dao

import org.apache.pekko.persistence.jdbc.TablesTestSpec
import slick.jdbc.JdbcProfile

class JournalTablesTest extends TablesTestSpec {
  val journalTableConfiguration = journalConfig.eventJournalTableConfiguration
  val tagTableConfiguration = journalConfig.eventTagTableConfiguration

  object TestJournalTables extends JournalTables {
    override val profile: JdbcProfile = slick.jdbc.PostgresProfile
    override val journalTableCfg = journalTableConfiguration
    override val tagTableCfg = tagTableConfiguration
  }

  "JournalTable" should "be configured with a schema name" in {
    TestJournalTables.JournalTable.baseTableRow.schemaName shouldBe journalTableConfiguration.schemaName
  }

  it should "be configured with a table name" in {
    TestJournalTables.JournalTable.baseTableRow.tableName shouldBe journalTableConfiguration.tableName
  }

  it should "be configured with column names" in {
    val colName = toColumnName(journalTableConfiguration.tableName)(_)
    val row = TestJournalTables.JournalTable.baseTableRow
    row.ordering.toString shouldBe colName(journalTableConfiguration.columnNames.ordering)
    row.deleted.toString shouldBe colName(journalTableConfiguration.columnNames.deleted)
    row.persistenceId.toString shouldBe colName(journalTableConfiguration.columnNames.persistenceId)
    row.sequenceNumber.toString shouldBe colName(journalTableConfiguration.columnNames.sequenceNumber)
    row.writer.toString shouldBe colName(journalTableConfiguration.columnNames.writer)
    row.timestamp.toString shouldBe colName(journalTableConfiguration.columnNames.writeTimestamp)
    row.adapterManifest.toString shouldBe colName(journalTableConfiguration.columnNames.adapterManifest)
    row.eventPayload.toString shouldBe colName(journalTableConfiguration.columnNames.eventPayload)
    row.eventSerId.toString shouldBe colName(journalTableConfiguration.columnNames.eventSerId)
    row.eventSerManifest.toString shouldBe colName(journalTableConfiguration.columnNames.eventSerManifest)
    row.metaPayload.toString shouldBe colName(journalTableConfiguration.columnNames.metaPayload)
    row.metaSerId.toString shouldBe colName(journalTableConfiguration.columnNames.metaSerId)
    row.metaSerManifest.toString shouldBe colName(journalTableConfiguration.columnNames.metaSerManifest)
  }

  "TagTable" should "be configured with a schema name" in {
    TestJournalTables.TagTable.baseTableRow.schemaName shouldBe tagTableConfiguration.schemaName
  }

  it should "be configured with a table name" in {
    TestJournalTables.TagTable.baseTableRow.tableName shouldBe tagTableConfiguration.tableName
  }

  it should "be configured with column names" in {
    val colName = toColumnName(tagTableConfiguration.tableName)(_)
    val row = TestJournalTables.TagTable.baseTableRow
    row.eventId.toString shouldBe colName(tagTableConfiguration.columnNames.eventId)
    row.persistenceId.toString shouldBe colName(tagTableConfiguration.columnNames.persistenceId)
    row.sequenceNumber.toString shouldBe colName(tagTableConfiguration.columnNames.sequenceNumber)
    row.tag.toString shouldBe colName(tagTableConfiguration.columnNames.tag)
  }
}
