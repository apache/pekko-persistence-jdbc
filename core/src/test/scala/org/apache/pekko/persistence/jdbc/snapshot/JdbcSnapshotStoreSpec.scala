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

package org.apache.pekko.persistence.jdbc.snapshot

import org.apache.pekko
import pekko.persistence.CapabilityFlag
import pekko.persistence.jdbc.config._
import pekko.persistence.jdbc.util.{ ClasspathResources, DropCreate }
import pekko.persistence.jdbc.db.SlickDatabase
import pekko.persistence.jdbc.testkit.internal.H2
import pekko.persistence.jdbc.testkit.internal.{ SchemaType, SchemaUtilsImpl }
import pekko.persistence.snapshot.SnapshotStoreSpec

import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

abstract class JdbcSnapshotStoreSpec(config: Config, schemaType: SchemaType)
    extends SnapshotStoreSpec(config)
    with BeforeAndAfterAll
    with ScalaFutures
    with ClasspathResources
    with DropCreate {
  implicit val pc: PatienceConfig = PatienceConfig(timeout = 10.seconds)

  implicit lazy val ec: ExecutionContext = system.dispatcher

  lazy val db = SlickDatabase.database(config, new SlickConfiguration(config.getConfig("slick")), "slick.db")

  protected override def supportsSerialization: CapabilityFlag = newDao
  protected override def supportsMetadata: CapabilityFlag = newDao

  override def beforeAll(): Unit = {
    dropAndCreate(schemaType)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    db.close()
    system.terminate().futureValue
  }
}

abstract class JdbcSnapshotStoreSchemaSpec(config: Config, schemaType: SchemaType)
    extends JdbcSnapshotStoreSpec(config, schemaType) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  protected def defaultSchemaName: String = "public"
  private val schemaName: String = "pekko"

  override def beforeAll(): Unit = {
    SchemaUtilsImpl.createWithSlickButChangeSchema(
      schemaType, logger, db, defaultSchemaName, schemaName)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    SchemaUtilsImpl.dropWithSlickButChangeSchema(
      schemaType, logger, db, defaultSchemaName, schemaName)
    super.afterAll()
  }

}

class H2SnapshotStoreSpec extends JdbcSnapshotStoreSpec(ConfigFactory.load("h2-application.conf"), H2)

object H2SnapshotStoreSchemaSpec {
  val config: Config = ConfigFactory.parseString("""
    jdbc-snapshot-store {
      tables {
        snapshot {
          schemaName = "pekko"
        }
      }
    }
  """).withFallback(
    ConfigFactory.load("h2-application.conf"))
}

class H2SnapshotStoreSchemaSpec extends JdbcSnapshotStoreSchemaSpec(H2SnapshotStoreSchemaSpec.config, H2) {
  override protected def defaultSchemaName: String = "PUBLIC"
}
