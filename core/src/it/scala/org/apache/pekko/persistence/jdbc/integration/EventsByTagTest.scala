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
  EventsByTagTest,
  MysqlCleaner,
  OracleCleaner,
  PostgresCleaner,
  SqlServerCleaner
}

class PostgresScalaEventsByTagTest extends EventsByTagTest("postgres-application.conf") with PostgresCleaner

class MySQLScalaEventByTagTest extends EventsByTagTest("mysql-application.conf") with MysqlCleaner

class OracleScalaEventByTagTest extends EventsByTagTest("oracle-application.conf") with OracleCleaner {
  override def timeoutMultiplier: Int = 4
}

class SqlServerScalaEventByTagTest extends EventsByTagTest("sqlserver-application.conf") with SqlServerCleaner
