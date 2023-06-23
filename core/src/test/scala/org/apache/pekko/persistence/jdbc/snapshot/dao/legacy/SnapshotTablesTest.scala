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

package org.apache.pekko.persistence.jdbc.snapshot.dao.legacy

import org.apache.pekko.persistence.jdbc.TablesTestSpec
import slick.jdbc.JdbcProfile

class SnapshotTablesTest extends TablesTestSpec {
  val snapshotTableConfiguration = snapshotConfig.legacySnapshotTableConfiguration
  object TestByteASnapshotTables extends SnapshotTables {
    override val profile: JdbcProfile = slick.jdbc.PostgresProfile
    override val snapshotTableCfg = snapshotTableConfiguration
  }

  "SnapshotTable" should "be configured with a schema name" in {
    TestByteASnapshotTables.SnapshotTable.baseTableRow.schemaName shouldBe snapshotTableConfiguration.schemaName
  }

  it should "be configured with a table name" in {
    TestByteASnapshotTables.SnapshotTable.baseTableRow.tableName shouldBe snapshotTableConfiguration.tableName
  }

  it should "be configured with column names" in {
    val colName = toColumnName(snapshotTableConfiguration.tableName)(_)
    TestByteASnapshotTables.SnapshotTable.baseTableRow.persistenceId.toString shouldBe colName(
      snapshotTableConfiguration.columnNames.persistenceId)
    TestByteASnapshotTables.SnapshotTable.baseTableRow.sequenceNumber.toString shouldBe colName(
      snapshotTableConfiguration.columnNames.sequenceNumber)
    TestByteASnapshotTables.SnapshotTable.baseTableRow.created.toString shouldBe colName(
      snapshotTableConfiguration.columnNames.created)
    TestByteASnapshotTables.SnapshotTable.baseTableRow.snapshot.toString shouldBe colName(
      snapshotTableConfiguration.columnNames.snapshot)
  }
}
