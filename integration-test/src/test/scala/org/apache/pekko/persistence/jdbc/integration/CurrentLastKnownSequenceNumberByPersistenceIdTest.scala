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
  CurrentLastSequenceNumberByPersistenceIdTest,
  MysqlCleaner,
  OracleCleaner,
  PostgresCleaner,
  SqlServerCleaner
}

// Note: these tests use the shared-db configs, the test for all (so not only current) events use the regular db config

class PostgresScalaCurrentLastSequenceNumberByPersistenceIdTest
    extends CurrentLastSequenceNumberByPersistenceIdTest("postgres-shared-db-application.conf")
    with PostgresCleaner

class MySQLScalaCurrentLastSequenceNumberByPersistenceIdTest
    extends CurrentLastSequenceNumberByPersistenceIdTest("mysql-shared-db-application.conf")
    with MysqlCleaner

class OracleScalaCurrentLastSequenceNumberByPersistenceIdTest
    extends CurrentLastSequenceNumberByPersistenceIdTest("oracle-shared-db-application.conf")
    with OracleCleaner

class SqlServerScalaCurrentLastSequenceNumberByPersistenceIdTest
    extends CurrentLastSequenceNumberByPersistenceIdTest("sqlserver-shared-db-application.conf")
    with SqlServerCleaner
