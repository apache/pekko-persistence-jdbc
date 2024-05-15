/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.integration

import org.apache.pekko
import pekko.persistence.jdbc.migrator.MigratorSpec._
import pekko.persistence.jdbc.migrator.JournalMigratorTest

class PostgresJournalMigratorTest extends JournalMigratorTest("postgres-application.conf") with PostgresCleaner

class MySQLJournalMigratorTest extends JournalMigratorTest("mysql-application.conf") with MysqlCleaner

class OracleJournalMigratorTest extends JournalMigratorTest("oracle-application.conf") with OracleCleaner

class SqlServerJournalMigratorTest extends JournalMigratorTest("sqlserver-application.conf") with SqlServerCleaner
