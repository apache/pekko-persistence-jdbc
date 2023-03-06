/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.integration

import org.apache.pekko.persistence.jdbc.journal.JdbcJournalPerfSpec
import org.apache.pekko.persistence.jdbc.testkit.internal.MySQL
import org.apache.pekko.persistence.jdbc.testkit.internal.Oracle
import org.apache.pekko.persistence.jdbc.testkit.internal.Postgres
import org.apache.pekko.persistence.jdbc.testkit.internal.SqlServer
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory

class PostgresJournalPerfSpec extends JdbcJournalPerfSpec(ConfigFactory.load("postgres-application.conf"), Postgres) {
  override def eventsCount: Int = 100
}

class PostgresJournalPerfSpecSharedDb
    extends JdbcJournalPerfSpec(ConfigFactory.load("postgres-shared-db-application.conf"), Postgres) {
  override def eventsCount: Int = 100
}

class MySQLJournalPerfSpec extends JdbcJournalPerfSpec(ConfigFactory.load("mysql-application.conf"), MySQL) {
  override def eventsCount: Int = 100
}

class MySQLJournalPerfSpecSharedDb
    extends JdbcJournalPerfSpec(ConfigFactory.load("mysql-shared-db-application.conf"), MySQL) {
  override def eventsCount: Int = 100
}

class OracleJournalPerfSpec extends JdbcJournalPerfSpec(ConfigFactory.load("oracle-application.conf"), Oracle) {
  override def eventsCount: Int = 100
}

class OracleJournalPerfSpecSharedDb
    extends JdbcJournalPerfSpec(ConfigFactory.load("oracle-shared-db-application.conf"), Oracle) {
  override def eventsCount: Int = 100
}

class SqlServerJournalPerfSpec
    extends JdbcJournalPerfSpec(ConfigFactory.load("sqlserver-application.conf"), SqlServer) {
  override def eventsCount: Int = 100
}

class SqlServerJournalPerfSpecSharedDb
    extends JdbcJournalPerfSpec(ConfigFactory.load("sqlserver-shared-db-application.conf"), SqlServer) {
  override def eventsCount: Int = 100
}
