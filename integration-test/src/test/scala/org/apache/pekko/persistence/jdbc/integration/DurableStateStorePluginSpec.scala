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
import org.apache.pekko.persistence.jdbc.state.scaladsl.DurableStateStorePluginSpec
import slick.jdbc.{ MySQLProfile, OracleProfile, PostgresProfile, SQLServerProfile }

class PostgresDurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("postgres-shared-db-application.conf"), PostgresProfile) {}

class OracleDurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("oracle-shared-db-application.conf"), OracleProfile) {}

class SqlServerDurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("sqlserver-shared-db-application.conf"), SQLServerProfile) {}
