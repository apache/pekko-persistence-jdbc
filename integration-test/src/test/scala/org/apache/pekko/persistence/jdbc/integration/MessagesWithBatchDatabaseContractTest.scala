/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.integration

import org.apache.pekko.persistence.jdbc.journal.dao.MessagesWithBatchDatabaseContractTest
import org.apache.pekko.persistence.jdbc.query.{
  MariaDBCleaner,
  MysqlCleaner,
  OracleCleaner,
  PostgresCleaner,
  SqlServerCleaner
}

final class PostgresMessagesWithBatchDatabaseContractTest
    extends MessagesWithBatchDatabaseContractTest("postgres-application.conf")
    with PostgresCleaner

final class MySQLMessagesWithBatchDatabaseContractTest
    extends MessagesWithBatchDatabaseContractTest("mysql-application.conf")
    with MysqlCleaner

final class MariaDBMessagesWithBatchDatabaseContractTest
    extends MessagesWithBatchDatabaseContractTest("mariadb-application.conf")
    with MariaDBCleaner

final class OracleMessagesWithBatchDatabaseContractTest
    extends MessagesWithBatchDatabaseContractTest("oracle-application.conf")
    with OracleCleaner

final class SqlServerMessagesWithBatchDatabaseContractTest
    extends MessagesWithBatchDatabaseContractTest("sqlserver-application.conf")
    with SqlServerCleaner
