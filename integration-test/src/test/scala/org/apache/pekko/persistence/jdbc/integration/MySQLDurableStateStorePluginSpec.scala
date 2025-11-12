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
import org.apache.pekko.persistence.jdbc.state.scaladsl.{DurableStateStorePluginSpec, DurableStateStoreSchemaPluginSpec}
import slick.jdbc.MySQLProfile

class MySQLDurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("mysql-shared-db-application.conf"), MySQLProfile) {}

class MySQLDurableStateStorePluginSchemaSpec
    extends DurableStateStoreSchemaPluginSpec(ConfigFactory.load("mysql-application.conf"),
      MySQLProfile) {}
