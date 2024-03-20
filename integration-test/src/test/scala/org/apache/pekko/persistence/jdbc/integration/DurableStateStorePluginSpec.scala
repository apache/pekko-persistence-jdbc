package org.apache.pekko.persistence.jdbc.integration

import com.typesafe.config.ConfigFactory
import org.apache.pekko.persistence.jdbc.state.scaladsl.DurableStateStorePluginSpec
import slick.jdbc.{ MySQLProfile, OracleProfile, PostgresProfile, SQLServerProfile }

class PostgresDurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("postgres-shared-db-application.conf"), PostgresProfile) {}

class MySQLDurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("mysql-shared-db-application.conf"), MySQLProfile) {}

class OracleDurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("oracle-shared-db-application.conf"), OracleProfile) {}

class SqlServerDurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("sqlserver-shared-db-application.conf"), SQLServerProfile) {}
