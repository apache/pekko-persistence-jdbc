/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.snapshot

import org.apache.pekko.persistence.CapabilityFlag
import org.apache.pekko.persistence.jdbc.config._
import org.apache.pekko.persistence.jdbc.util.{ ClasspathResources, DropCreate }
import org.apache.pekko.persistence.jdbc.db.SlickDatabase
import org.apache.pekko.persistence.snapshot.SnapshotStoreSpec
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._

import org.apache.pekko.persistence.jdbc.testkit.internal.H2
import org.apache.pekko.persistence.jdbc.testkit.internal.SchemaType

abstract class JdbcSnapshotStoreSpec(config: Config, schemaType: SchemaType)
    extends SnapshotStoreSpec(config)
    with BeforeAndAfterAll
    with ScalaFutures
    with ClasspathResources
    with DropCreate {
  implicit val pc: PatienceConfig = PatienceConfig(timeout = 10.seconds)

  implicit lazy val ec = system.dispatcher

  lazy val cfg = system.settings.config.getConfig("jdbc-journal")

  lazy val journalConfig = new JournalConfig(cfg)

  lazy val db = SlickDatabase.database(cfg, new SlickConfiguration(cfg.getConfig("slick")), "slick.db")

  protected override def supportsSerialization: CapabilityFlag = newDao
  protected override def supportsMetadata: CapabilityFlag = newDao

  override def beforeAll(): Unit = {
    dropAndCreate(schemaType)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    db.close()
  }
}

class H2SnapshotStoreSpec extends JdbcSnapshotStoreSpec(ConfigFactory.load("h2-application.conf"), H2)
