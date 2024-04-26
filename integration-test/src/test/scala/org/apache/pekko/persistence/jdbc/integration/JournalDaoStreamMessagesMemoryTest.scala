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
  LegacyJournalDaoStreamMessagesMemoryTest,
  MysqlCleaner,
  OracleCleaner,
  PostgresCleaner,
  SqlServerCleaner
}

class PostgresLegacyJournalDaoStreamMessagesMemoryTest
    extends LegacyJournalDaoStreamMessagesMemoryTest("postgres-application.conf")
    with PostgresCleaner

class MySQLLegacyJournalDaoStreamMessagesMemoryTest
    extends LegacyJournalDaoStreamMessagesMemoryTest("mysql-application.conf")
    with MysqlCleaner

class OracleLegacyJournalDaoStreamMessagesMemoryTest
    extends LegacyJournalDaoStreamMessagesMemoryTest("oracle-application.conf")
    with OracleCleaner

class SqlServerLegacyJournalDaoStreamMessagesMemoryTest
    extends LegacyJournalDaoStreamMessagesMemoryTest("sqlserver-application.conf")
    with SqlServerCleaner
