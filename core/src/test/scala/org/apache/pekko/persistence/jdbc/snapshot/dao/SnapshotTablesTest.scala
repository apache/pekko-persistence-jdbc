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

package org.apache.pekko.persistence.jdbc.snapshot.dao

import org.apache.pekko.persistence.jdbc.TablesTestSpec
import slick.jdbc.JdbcProfile

class SnapshotTablesTest extends TablesTestSpec {
  val snapshotTableConfiguration = snapshotConfig.snapshotTableConfiguration

  object TestSnapshotTables extends SnapshotTables {
    override val profile: JdbcProfile = slick.jdbc.PostgresProfile
    override val snapshotTableCfg = snapshotTableConfiguration
  }

  "SnapshotTable" should "be configured with a schema name" in {
    TestSnapshotTables.SnapshotTable.baseTableRow.schemaName shouldBe snapshotTableConfiguration.schemaName
  }

  it should "be configured with a table name" in {
    TestSnapshotTables.SnapshotTable.baseTableRow.tableName shouldBe snapshotTableConfiguration.tableName
  }

  it should "be configured with column names" in {
    val colName = toColumnName(snapshotTableConfiguration.tableName)(_)
    val row = TestSnapshotTables.SnapshotTable.baseTableRow
    row.persistenceId.toString shouldBe colName(snapshotTableConfiguration.columnNames.persistenceId)
    row.sequenceNumber.toString shouldBe colName(snapshotTableConfiguration.columnNames.sequenceNumber)
    row.created.toString shouldBe colName(snapshotTableConfiguration.columnNames.created)
    row.snapshotPayload.toString shouldBe colName(snapshotTableConfiguration.columnNames.snapshotPayload)
    row.snapshotSerId.toString shouldBe colName(snapshotTableConfiguration.columnNames.snapshotSerId)
    row.snapshotSerManifest.toString shouldBe colName(snapshotTableConfiguration.columnNames.snapshotSerManifest)
    row.metaPayload.toString shouldBe colName(snapshotTableConfiguration.columnNames.metaPayload)
    row.metaSerId.toString shouldBe colName(snapshotTableConfiguration.columnNames.metaSerId)
    row.metaSerManifest.toString shouldBe colName(snapshotTableConfiguration.columnNames.metaSerManifest)
  }
}
