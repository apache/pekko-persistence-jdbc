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
package org.apache.pekko.persistence.jdbc.integration

import org.apache.pekko.persistence.jdbc.query.{
  EventsByTagMigrationTest,
  MysqlCleaner,
  OracleCleaner,
  PostgresCleaner,
  SqlServerCleaner
}

class PostgresScalaEventsByTagMigrationTest
    extends EventsByTagMigrationTest("postgres-application.conf")
    with PostgresCleaner {}

class MySQLScalaEventByTagMigrationTest extends EventsByTagMigrationTest("mysql-application.conf") with MysqlCleaner {

  override def dropLegacyFKConstraint(): Unit =
    dropConstraint(constraintType = "FOREIGN KEY", constraintDialect = "FOREIGN KEY")

  override def dropLegacyPKConstraint(): Unit =
    dropConstraint(constraintType = "PRIMARY KEY", constraintDialect = "", constraintNameDialect = "KEY")

  override def addNewPKConstraint(): Unit =
    addPKConstraint(constraintNameDialect = "")

  override def addNewFKConstraint(): Unit =
    addFKConstraint()

  override def migrateLegacyRows(): Unit =
    fillNewColumn(
      joinDialect = joinSQL,
      pidSetDialect =
        s"${tagTableCfg.tableName}.${tagTableCfg.columnNames.persistenceId} = ${journalTableName}.${journalTableCfg.columnNames.persistenceId}",
      seqNrSetDialect =
        s"${tagTableCfg.tableName}.${tagTableCfg.columnNames.sequenceNumber} = ${journalTableName}.${journalTableCfg.columnNames.sequenceNumber}")
}

class OracleScalaEventByTagMigrationTest
    extends EventsByTagMigrationTest("oracle-application.conf")
    with OracleCleaner {

  override def addNewColumn(): Unit = {
    // mock event_id not null, in order to change it to null later
    alterColumn(alterDialect = "MODIFY", changeToDialect = "NOT NULL")
  }

  override def dropLegacyFKConstraint(): Unit =
    dropConstraint(constraintTableName = "USER_CONSTRAINTS", constraintType = "R")

  override def dropLegacyPKConstraint(): Unit =
    dropConstraint(constraintTableName = "USER_CONSTRAINTS", constraintType = "P")

  override def migrateLegacyRows(): Unit =
    withStatement { stmt =>
      stmt.execute(s"""UPDATE ${tagTableCfg.tableName}
                      |SET (${tagTableCfg.columnNames.persistenceId}, ${tagTableCfg.columnNames.sequenceNumber}) = (
                      |    SELECT ${journalTableCfg.columnNames.persistenceId}, ${journalTableCfg.columnNames.sequenceNumber}
                      |    ${fromSQL}
                      |)
                      |WHERE EXISTS (
                      |    SELECT 1
                      |    ${fromSQL}
                      |)""".stripMargin)
    }
}

class SqlServerScalaEventByTagMigrationTest
    extends EventsByTagMigrationTest("sqlserver-application.conf")
    with SqlServerCleaner {

  override def addNewPKConstraint(): Unit = {
    // Change new column not null
    alterColumn(columnName = tagTableCfg.columnNames.persistenceId, changeToDialect = "NVARCHAR(255) NOT NULL")
    alterColumn(columnName = tagTableCfg.columnNames.sequenceNumber, changeToDialect = "NUMERIC(10,0) NOT NULL")
    super.addNewPKConstraint()
  }
}
