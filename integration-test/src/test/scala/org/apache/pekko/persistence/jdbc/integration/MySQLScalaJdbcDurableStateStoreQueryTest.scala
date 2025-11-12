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
import org.apache.pekko
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.jdbc.state.scaladsl.JdbcDurableStateSpec
import org.apache.pekko.persistence.jdbc.testkit.internal.MySQL

class MySQLScalaJdbcDurableStateStoreQueryTest
    extends JdbcDurableStateSpec(ConfigFactory.load("mysql-shared-db-application.conf"), MySQL) {
  implicit lazy val system: ActorSystem =
    ActorSystem("MySQLScalaJdbcDurableStateStoreQueryTest", config.withFallback(customSerializers))
}
