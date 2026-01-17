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
import slick.jdbc.PostgresProfile
import org.apache.pekko.persistence.jdbc.state.scaladsl.{
  DurableStateStorePluginSpec, DurableStateStoreSchemaPluginSpec
}

class PostgresDurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("postgres-shared-db-application.conf"), PostgresProfile) {}

class PostgresDurableStateStorePluginSchemaSpec
    extends DurableStateStoreSchemaPluginSpec(ConfigFactory.load("postgres-application.conf"),
      PostgresProfile) {}
