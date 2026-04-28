/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.integration

import com.typesafe.config.ConfigFactory
import org.apache.pekko.persistence.jdbc.state.scaladsl.JdbcDurableStateStoreTCKSpec
import org.apache.pekko.persistence.jdbc.testkit.internal.{ MariaDB, MySQL, Oracle, Postgres, SqlServer }

// postgres-application.conf already includes pekko.persistence.state.plugin = "jdbc-durable-state-store"
class PostgresDurableStateStoreTCKSpec
    extends JdbcDurableStateStoreTCKSpec(ConfigFactory.load("postgres-application.conf"), Postgres)

// mariadb-application.conf already includes pekko.persistence.state.plugin = "jdbc-durable-state-store"
class MariaDBDurableStateStoreTCKSpec
    extends JdbcDurableStateStoreTCKSpec(ConfigFactory.load("mariadb-application.conf"), MariaDB)

object MySQLDurableStateStoreTCKSpec {
  // mysql-application.conf does not configure the durable state plugin, so add it here
  val config = ConfigFactory
    .parseString("""pekko.persistence.state.plugin = "jdbc-durable-state-store"""")
    .withFallback(ConfigFactory.load("mysql-application.conf"))
}

class MySQLDurableStateStoreTCKSpec
    extends JdbcDurableStateStoreTCKSpec(MySQLDurableStateStoreTCKSpec.config, MySQL)

object OracleDurableStateStoreTCKSpec {
  // oracle-application.conf does not configure the durable state plugin, so add it here
  val config = ConfigFactory
    .parseString("""pekko.persistence.state.plugin = "jdbc-durable-state-store"""")
    .withFallback(ConfigFactory.load("oracle-application.conf"))
}

class OracleDurableStateStoreTCKSpec
    extends JdbcDurableStateStoreTCKSpec(OracleDurableStateStoreTCKSpec.config, Oracle)

object SqlServerDurableStateStoreTCKSpec {
  // sqlserver-application.conf does not configure the durable state plugin, so add it here
  val config = ConfigFactory
    .parseString("""pekko.persistence.state.plugin = "jdbc-durable-state-store"""")
    .withFallback(ConfigFactory.load("sqlserver-application.conf"))
}

class SqlServerDurableStateStoreTCKSpec
    extends JdbcDurableStateStoreTCKSpec(SqlServerDurableStateStoreTCKSpec.config, SqlServer)
