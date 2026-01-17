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
  EventAdapterTest, MariaDBCleaner, MysqlCleaner, OracleCleaner, PostgresCleaner, SqlServerCleaner
}

class PostgresScalaEventAdapterTest extends EventAdapterTest("postgres-application.conf") with PostgresCleaner

class MySQLScalaEventAdapterTest extends EventAdapterTest("mysql-application.conf") with MysqlCleaner

class MariaDBScalaEventAdapterTest extends EventAdapterTest("mariadb-application.conf") with MariaDBCleaner

class OracleScalaEventAdapterTest extends EventAdapterTest("oracle-application.conf") with OracleCleaner

class SqlServerScalaEventAdapterTest extends EventAdapterTest("sqlserver-application.conf") with SqlServerCleaner
