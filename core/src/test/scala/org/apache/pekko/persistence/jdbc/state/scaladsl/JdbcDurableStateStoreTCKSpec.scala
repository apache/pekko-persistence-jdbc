/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.state.scaladsl

import com.typesafe.config.Config
import org.apache.pekko
import pekko.persistence.CapabilityFlag
import pekko.persistence.jdbc.config.SlickConfiguration
import pekko.persistence.jdbc.db.SlickDatabase
import pekko.persistence.jdbc.testkit.internal.SchemaType
import pekko.persistence.jdbc.util.DropCreate
import pekko.persistence.state.DurableStateStoreSpec
import org.scalatest.BeforeAndAfterAll

abstract class JdbcDurableStateStoreTCKSpec(config: Config, schemaType: SchemaType)
    extends DurableStateStoreSpec(config)
    with BeforeAndAfterAll
    with DropCreate {

  override protected def supportsDeleteWithRevisionCheck: CapabilityFlag = CapabilityFlag.on()

  lazy val db = {
    val cfg = config.getConfig("jdbc-durable-state-store")
    if (cfg.hasPath("slick.profile")) {
      SlickDatabase.database(cfg, new SlickConfiguration(cfg.getConfig("slick")), "slick.db")
    } else {
      SlickDatabase.database(
        config,
        new SlickConfiguration(config.getConfig("pekko-persistence-jdbc.shared-databases.slick")),
        "pekko-persistence-jdbc.shared-databases.slick.db")
    }
  }

  override def beforeAll(): Unit = {
    dropAndCreate(schemaType)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    db.close()
  }
}
