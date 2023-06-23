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
import pekko.persistence.jdbc.snapshot.JdbcSnapshotStoreSpec
import pekko.persistence.jdbc.testkit.internal.MySQL
import pekko.persistence.jdbc.testkit.internal.Oracle
import pekko.persistence.jdbc.testkit.internal.Postgres
import pekko.persistence.jdbc.testkit.internal.SqlServer
import com.typesafe.config.ConfigFactory

class PostgresSnapshotStoreSpec extends JdbcSnapshotStoreSpec(ConfigFactory.load("postgres-application.conf"), Postgres)

class MySQLSnapshotStoreSpec extends JdbcSnapshotStoreSpec(ConfigFactory.load("mysql-application.conf"), MySQL)

class OracleSnapshotStoreSpec extends JdbcSnapshotStoreSpec(ConfigFactory.load("oracle-application.conf"), Oracle)

class SqlServerSnapshotStoreSpec
    extends JdbcSnapshotStoreSpec(ConfigFactory.load("sqlserver-application.conf"), SqlServer)
