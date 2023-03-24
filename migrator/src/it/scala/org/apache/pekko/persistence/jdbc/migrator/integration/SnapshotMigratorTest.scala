/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.migrator.integration

import org.apache.pekko
import pekko.persistence.jdbc.migrator.MigratorSpec._
import pekko.persistence.jdbc.migrator.SnapshotMigratorTest

class PostgresSnapshotMigratorTest extends SnapshotMigratorTest("postgres-application.conf") with PostgresCleaner

class MySQLSnapshotMigratorTest extends SnapshotMigratorTest("mysql-application.conf") with MysqlCleaner

class OracleSnapshotMigratorTest extends SnapshotMigratorTest("oracle-application.conf") with OracleCleaner

class SqlServerSnapshotMigratorTest extends SnapshotMigratorTest("sqlserver-application.conf") with SqlServerCleaner
