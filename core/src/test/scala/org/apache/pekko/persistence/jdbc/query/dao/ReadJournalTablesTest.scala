/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.query.dao

import org.apache.pekko
import pekko.persistence.jdbc.TablesTestSpec
import pekko.persistence.jdbc.journal.dao.legacy.JournalTables
import slick.jdbc.JdbcProfile

class ReadJournalTablesTest extends TablesTestSpec {
  val readJournalTableConfiguration = readJournalConfig.journalTableConfiguration

  object TestByteAReadJournalTables extends JournalTables {
    override val profile: JdbcProfile = slick.jdbc.PostgresProfile
    override val journalTableCfg = readJournalTableConfiguration
  }

  "JournalTable" should "be configured with a schema name" in {
    TestByteAReadJournalTables.JournalTable.baseTableRow.schemaName shouldBe readJournalTableConfiguration.schemaName
  }

  it should "be configured with a table name" in {
    TestByteAReadJournalTables.JournalTable.baseTableRow.tableName shouldBe readJournalTableConfiguration.tableName
  }

  it should "be configured with column names" in {
    val colName = toColumnName(readJournalTableConfiguration.tableName)(_)
    TestByteAReadJournalTables.JournalTable.baseTableRow.persistenceId.toString shouldBe colName(
      readJournalTableConfiguration.columnNames.persistenceId)
    TestByteAReadJournalTables.JournalTable.baseTableRow.sequenceNumber.toString shouldBe colName(
      readJournalTableConfiguration.columnNames.sequenceNumber)
    //    TestByteAJournalTables.JournalTable.baseTableRow.tags.toString() shouldBe colName(journalTableConfiguration.columnNames.tags)
  }
}
