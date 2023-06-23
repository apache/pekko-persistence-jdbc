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
  HardDeleteQueryTest,
  MysqlCleaner,
  OracleCleaner,
  PostgresCleaner,
  SqlServerCleaner
}

class PostgresHardDeleteQueryTest extends HardDeleteQueryTest("postgres-application.conf") with PostgresCleaner

class MySQLHardDeleteQueryTest extends HardDeleteQueryTest("mysql-application.conf") with MysqlCleaner

class OracleHardDeleteQueryTest extends HardDeleteQueryTest("oracle-application.conf") with OracleCleaner

class SqlServerHardDeleteQueryTest extends HardDeleteQueryTest("sqlserver-application.conf") with SqlServerCleaner
