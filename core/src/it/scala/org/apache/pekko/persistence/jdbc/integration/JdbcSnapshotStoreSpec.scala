package org.apache.pekko.persistence.jdbc.integration

import org.apache.pekko.persistence.jdbc.snapshot.JdbcSnapshotStoreSpec
import org.apache.pekko.persistence.jdbc.testkit.internal.MySQL
import org.apache.pekko.persistence.jdbc.testkit.internal.Oracle
import org.apache.pekko.persistence.jdbc.testkit.internal.Postgres
import org.apache.pekko.persistence.jdbc.testkit.internal.SqlServer
import com.typesafe.config.ConfigFactory

class PostgresSnapshotStoreSpec extends JdbcSnapshotStoreSpec(ConfigFactory.load("postgres-application.conf"), Postgres)

class MySQLSnapshotStoreSpec extends JdbcSnapshotStoreSpec(ConfigFactory.load("mysql-application.conf"), MySQL)

class OracleSnapshotStoreSpec extends JdbcSnapshotStoreSpec(ConfigFactory.load("oracle-application.conf"), Oracle)

class SqlServerSnapshotStoreSpec
    extends JdbcSnapshotStoreSpec(ConfigFactory.load("sqlserver-application.conf"), SqlServer)
