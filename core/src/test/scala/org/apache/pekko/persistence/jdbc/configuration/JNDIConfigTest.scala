/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.configuration

import com.typesafe.config.ConfigFactory
import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.persistence.jdbc.SimpleSpec
import pekko.persistence.jdbc.db.SlickExtension

class JNDIConfigTest extends SimpleSpec {
  "JNDI config" should "read the config and throw NoInitialContextException in case the JNDI resource is not available" in {
    withActorSystem("jndi-application.conf") { system =>
      val jdbcJournalConfig = system.settings.config.getConfig("jdbc-journal")
      val slickExtension = SlickExtension(system)
      intercept[javax.naming.NoInitialContextException] {
        // Since the JNDI resource is not actually available we expect a NoInitialContextException
        // This is an indication that the application actually attempts to load the configured JNDI resource
        slickExtension.database(jdbcJournalConfig).database
      }
    }
  }

  "JNDI config for shared databases" should "read the config and throw NoInitialContextException in case the JNDI resource is not available" in {
    withActorSystem("jndi-shared-db-application.conf") { system =>
      val jdbcJournalConfig = system.settings.config.getConfig("jdbc-journal")
      val slickExtension = SlickExtension(system)
      intercept[javax.naming.NoInitialContextException] {
        // Since the JNDI resource is not actually available we expect a NoInitialContextException
        // This is an indication that the application actually attempts to load the configured JNDI resource
        slickExtension.database(jdbcJournalConfig).database
      }
    }
  }

  def withActorSystem(config: String)(f: ActorSystem => Unit): Unit = {
    val cfg = ConfigFactory.load(config)
    val system = ActorSystem("test", cfg)

    try {
      f(system)
    } finally {
      system.terminate().futureValue
    }
  }
}
