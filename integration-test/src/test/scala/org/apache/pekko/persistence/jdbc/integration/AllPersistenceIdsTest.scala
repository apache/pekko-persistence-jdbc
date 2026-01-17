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
  AllPersistenceIdsTest, MariaDBCleaner, MysqlCleaner, OracleCleaner, PostgresCleaner, SqlServerCleaner
}

class PostgresScalaAllPersistenceIdsTest extends AllPersistenceIdsTest("postgres-application.conf") with PostgresCleaner

class MySQLScalaAllPersistenceIdsTest extends AllPersistenceIdsTest("mysql-application.conf") with MysqlCleaner

class MariaDBScalaAllPersistenceIdsTest extends AllPersistenceIdsTest("mariadb-application.conf") with MariaDBCleaner

class OracleScalaAllPersistenceIdsTest extends AllPersistenceIdsTest("oracle-application.conf") with OracleCleaner

class SqlServerScalaAllPersistenceIdsTest
    extends AllPersistenceIdsTest("sqlserver-application.conf")
    with SqlServerCleaner
