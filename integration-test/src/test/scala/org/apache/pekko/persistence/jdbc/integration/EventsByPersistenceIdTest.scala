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
  EventsByPersistenceIdTest, MariaDBCleaner, MysqlCleaner, OracleCleaner, PostgresCleaner, SqlServerCleaner
}

class PostgresScalaEventsByPersistenceIdTest
    extends EventsByPersistenceIdTest("postgres-application.conf")
    with PostgresCleaner

class MySQLScalaEventsByPersistenceIdTest extends EventsByPersistenceIdTest("mysql-application.conf") with MysqlCleaner

class MariaDBScalaEventsByPersistenceIdTest extends EventsByPersistenceIdTest("mariadb-application.conf")
    with MariaDBCleaner

class OracleScalaEventsByPersistenceIdTest
    extends EventsByPersistenceIdTest("oracle-application.conf")
    with OracleCleaner

class SqlServerScalaEventsByPersistenceIdTest
    extends EventsByPersistenceIdTest("sqlserver-application.conf")
    with SqlServerCleaner
