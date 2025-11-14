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
  JournalDaoStreamMessagesMemoryTest,
  MariaDBCleaner,
  MysqlCleaner,
  OracleCleaner,
  PostgresCleaner,
  SqlServerCleaner
}

class PostgresJournalDaoStreamMessagesMemoryTest
    extends JournalDaoStreamMessagesMemoryTest("postgres-application.conf")
    with PostgresCleaner

class MySQLJournalDaoStreamMessagesMemoryTest
    extends JournalDaoStreamMessagesMemoryTest("mysql-application.conf")
    with MysqlCleaner

class MariaDBJournalDaoStreamMessagesMemoryTest
    extends JournalDaoStreamMessagesMemoryTest("mariadb-application.conf")
    with MariaDBCleaner

class OracleJournalDaoStreamMessagesMemoryTest
    extends JournalDaoStreamMessagesMemoryTest("oracle-application.conf")
    with OracleCleaner

class SqlServerJournalDaoStreamMessagesMemoryTest
    extends JournalDaoStreamMessagesMemoryTest("sqlserver-application.conf")
    with SqlServerCleaner
