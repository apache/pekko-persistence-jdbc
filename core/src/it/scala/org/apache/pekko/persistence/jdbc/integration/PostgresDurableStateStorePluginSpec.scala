package org.apache.pekko.persistence.jdbc.integration

import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile
import org.apache.pekko.persistence.jdbc.state.scaladsl.DurableStateStorePluginSpec

class PostgresDurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("postgres-shared-db-application.conf"), PostgresProfile) {}
