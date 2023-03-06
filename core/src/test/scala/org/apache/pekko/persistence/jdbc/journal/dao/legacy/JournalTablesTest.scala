/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.journal.dao.legacy

import org.apache.pekko.persistence.jdbc.TablesTestSpec
import slick.jdbc.JdbcProfile

class JournalTablesTest extends TablesTestSpec {
  val journalTableConfiguration = journalConfig.journalTableConfiguration

  object TestByteAJournalTables extends JournalTables {
    override val profile: JdbcProfile = slick.jdbc.PostgresProfile
    override val journalTableCfg = journalTableConfiguration
  }

  "JournalTable" should "be configured with a schema name" in {
    TestByteAJournalTables.JournalTable.baseTableRow.schemaName shouldBe journalTableConfiguration.schemaName
  }

  it should "be configured with a table name" in {
    TestByteAJournalTables.JournalTable.baseTableRow.tableName shouldBe journalTableConfiguration.tableName
  }

  it should "be configured with column names" in {
    val colName = toColumnName(journalTableConfiguration.tableName)(_)
    TestByteAJournalTables.JournalTable.baseTableRow.persistenceId.toString shouldBe colName(
      journalTableConfiguration.columnNames.persistenceId)
    TestByteAJournalTables.JournalTable.baseTableRow.deleted.toString shouldBe colName(
      journalTableConfiguration.columnNames.deleted)
    TestByteAJournalTables.JournalTable.baseTableRow.sequenceNumber.toString shouldBe colName(
      journalTableConfiguration.columnNames.sequenceNumber)
    //    TestByteAJournalTables.JournalTable.baseTableRow.tags.toString() shouldBe colName(journalTableConfiguration.columnNames.tags)
  }
}
