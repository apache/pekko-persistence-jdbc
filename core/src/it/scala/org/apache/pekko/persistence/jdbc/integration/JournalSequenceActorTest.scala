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
  JournalSequenceActorTest,
  MysqlCleaner,
  OracleCleaner,
  PostgresCleaner,
  SqlServerCleaner
}

class PostgresJournalSequenceActorTest
    extends JournalSequenceActorTest("postgres-application.conf", isOracle = false)
    with PostgresCleaner

class MySQLJournalSequenceActorTest
    extends JournalSequenceActorTest("mysql-application.conf", isOracle = false)
    with MysqlCleaner

class OracleJournalSequenceActorTest
    extends JournalSequenceActorTest("oracle-application.conf", isOracle = true)
    with OracleCleaner

class SqlServerJournalSequenceActorTest
    extends JournalSequenceActorTest("sqlserver-application.conf", isOracle = false)
    with SqlServerCleaner
