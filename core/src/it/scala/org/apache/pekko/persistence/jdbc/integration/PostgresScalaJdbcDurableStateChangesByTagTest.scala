package org.apache.pekko.persistence.jdbc.integration

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.jdbc.state.scaladsl.JdbcDurableStateSpec
import org.apache.pekko.persistence.jdbc.testkit.internal.Postgres

class PostgresScalaJdbcDurableStateStoreQueryTest
    extends JdbcDurableStateSpec(ConfigFactory.load("postgres-shared-db-application.conf"), Postgres) {
  implicit lazy val system: ActorSystem =
    ActorSystem("JdbcDurableStateSpec", config.withFallback(customSerializers))
}
