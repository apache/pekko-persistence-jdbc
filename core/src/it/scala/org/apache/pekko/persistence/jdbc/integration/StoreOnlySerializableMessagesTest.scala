package org.apache.pekko.persistence.jdbc.integration

import org.apache.pekko.persistence.jdbc.serialization.StoreOnlySerializableMessagesTest
import org.apache.pekko.persistence.jdbc.testkit.internal.MySQL
import org.apache.pekko.persistence.jdbc.testkit.internal.Oracle
import org.apache.pekko.persistence.jdbc.testkit.internal.Postgres
import org.apache.pekko.persistence.jdbc.testkit.internal.SqlServer

class PostgresStoreOnlySerializableMessagesTest
    extends StoreOnlySerializableMessagesTest("postgres-application.conf", Postgres)

class MySQLStoreOnlySerializableMessagesTest extends StoreOnlySerializableMessagesTest("mysql-application.conf", MySQL)

class OracleStoreOnlySerializableMessagesTest
    extends StoreOnlySerializableMessagesTest("oracle-application.conf", Oracle)

class SqlServerStoreOnlySerializableMessagesTest
    extends StoreOnlySerializableMessagesTest("sqlserver-application.conf", SqlServer)
