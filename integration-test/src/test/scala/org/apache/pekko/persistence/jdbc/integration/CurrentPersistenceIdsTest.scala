/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.integration

import org.apache.pekko.persistence.jdbc.query.{
  CurrentPersistenceIdsTest,
  MysqlCleaner,
  OracleCleaner,
  PostgresCleaner,
  SqlServerCleaner
}

// Note: these tests use the shared-db configs, the test for all persistence ids use the regular db config
class PostgresScalaCurrentPersistenceIdsTest
    extends CurrentPersistenceIdsTest("postgres-shared-db-application.conf")
    with PostgresCleaner

class MySQLScalaCurrentPersistenceIdsTest
    extends CurrentPersistenceIdsTest("mysql-shared-db-application.conf")
    with MysqlCleaner

class OracleScalaCurrentPersistenceIdsTest
    extends CurrentPersistenceIdsTest("oracle-shared-db-application.conf")
    with OracleCleaner

class SqlServerScalaCurrentPersistenceIdsTest
    extends CurrentPersistenceIdsTest("sqlserver-application.conf")
    with SqlServerCleaner
