/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

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
