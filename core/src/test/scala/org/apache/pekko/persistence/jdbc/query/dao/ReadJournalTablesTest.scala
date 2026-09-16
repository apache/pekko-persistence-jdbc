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
import pekko.persistence.jdbc.journal.dao.JournalTables
import slick.jdbc.JdbcProfile

class ReadJournalTablesTest extends TablesTestSpec {
  val readJournalTableConfiguration = readJournalConfig.eventJournalTableConfiguration

  object TestReadJournalTables extends JournalTables {
    override val profile: JdbcProfile = slick.jdbc.PostgresProfile
    override val journalTableCfg = readJournalTableConfiguration
    override val tagTableCfg = readJournalConfig.eventTagTableConfiguration
  }

  "JournalTable" should "be configured with a schema name" in {
    TestReadJournalTables.JournalTable.baseTableRow.schemaName shouldBe readJournalTableConfiguration.schemaName
  }

  it should "be configured with a table name" in {
    TestReadJournalTables.JournalTable.baseTableRow.tableName shouldBe readJournalTableConfiguration.tableName
  }

  it should "be configured with column names" in {
    val colName = toColumnName(readJournalTableConfiguration.tableName)(_)
    val row = TestReadJournalTables.JournalTable.baseTableRow
    row.ordering.toString shouldBe colName(readJournalTableConfiguration.columnNames.ordering)
    row.persistenceId.toString shouldBe colName(readJournalTableConfiguration.columnNames.persistenceId)
    row.sequenceNumber.toString shouldBe colName(readJournalTableConfiguration.columnNames.sequenceNumber)
    row.eventPayload.toString shouldBe colName(readJournalTableConfiguration.columnNames.eventPayload)
  }
}
