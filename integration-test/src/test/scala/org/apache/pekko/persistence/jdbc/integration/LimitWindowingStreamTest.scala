/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.pekko.persistence.jdbc.integration

import org.apache.pekko.persistence.jdbc.journal.dao.LimitWindowingStreamTest
import org.apache.pekko.persistence.jdbc.query.{
  MariaDBCleaner,
  MysqlCleaner,
  OracleCleaner,
  PostgresCleaner,
  SqlServerCleaner
}

class PostgresLimitWindowingStreamTest
    extends LimitWindowingStreamTest("postgres-application.conf")
    with PostgresCleaner

class MySQLLimitWindowingStreamTest
    extends LimitWindowingStreamTest("mysql-application.conf")
    with MysqlCleaner

class MariaDBLimitWindowingStreamTest
    extends LimitWindowingStreamTest("mariadb-application.conf")
    with MariaDBCleaner

class OracleLimitWindowingStreamTest
    extends LimitWindowingStreamTest("oracle-application.conf")
    with OracleCleaner

class SqlServerLimitWindowingStreamTest
    extends LimitWindowingStreamTest("sqlserver-application.conf")
    with SqlServerCleaner
