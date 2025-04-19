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
import pekko.persistence.jdbc.snapshot.{ JdbcSnapshotStoreSchemaSpec, JdbcSnapshotStoreSpec }
import pekko.persistence.jdbc.testkit.internal.{ MySQL, Oracle, Postgres, SqlServer }
import com.typesafe.config.{ Config, ConfigFactory }

class PostgresSnapshotStoreSpec extends JdbcSnapshotStoreSpec(ConfigFactory.load("postgres-application.conf"), Postgres)

object PostgresSnapshotStoreSchemaSpec {
  val config: Config = ConfigFactory.parseString("""
    jdbc-snapshot-store {
      tables {
        snapshot {
          schemaName = "pekko"
        }
      }
    }
  """).withFallback(
    ConfigFactory.load("postgres-application.conf"))
}

class PostgresSnapshotStoreSchemaSpec
    extends JdbcSnapshotStoreSchemaSpec(PostgresSnapshotStoreSchemaSpec.config, Postgres)

class MySQLSnapshotStoreSpec
    extends JdbcSnapshotStoreSpec(ConfigFactory.load("mysql-application.conf"), MySQL)

class OracleSnapshotStoreSpec
    extends JdbcSnapshotStoreSpec(ConfigFactory.load("oracle-application.conf"), Oracle)

class SqlServerSnapshotStoreSpec
    extends JdbcSnapshotStoreSpec(ConfigFactory.load("sqlserver-application.conf"), SqlServer)
