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

import scala.concurrent.Future

import org.apache.pekko
import pekko.persistence.{ CapabilityFlag, SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria }
import pekko.persistence.SnapshotProtocol.{ DeleteSnapshots, LoadSnapshot, LoadSnapshotResult }
import pekko.persistence.DeleteSnapshotsSuccess
import pekko.persistence.jdbc.config._
import pekko.persistence.jdbc.util.{ ClasspathResources, DropCreate }
import pekko.persistence.jdbc.db.SlickDatabase
import pekko.persistence.jdbc.testkit.internal.{ H2, SchemaType, SchemaUtilsImpl }
import pekko.persistence.snapshot.SnapshotStoreSpec
import pekko.persistence.jdbc.snapshot.dao.DefaultSnapshotDao
import pekko.serialization.SerializationExtension
import pekko.stream.SystemMaterializer
import pekko.testkit.{ TestKit, TestProbe }

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

  protected override def supportsSerialization: CapabilityFlag = true
  protected override def supportsMetadata: CapabilityFlag = true

  private lazy val boundsDao: DefaultSnapshotDao = {
    val profile = SlickDatabase.profile(config, "slick")
    val snapshotConfig = new SnapshotConfig(system.settings.config.getConfig("jdbc-snapshot-store"))
    val serialization = SerializationExtension(system)
    implicit val mat = SystemMaterializer(system).materializer
    new DefaultSnapshotDao(db, profile, snapshotConfig, serialization)
  }

  private def seedBoundsSnapshots(persistenceId: String): Seq[SnapshotMetadata] = {
    // Deliberately do not order timestamps by sequence number.
    val metadata = Seq(100L, 400L, 200L, 300L, 500L).zipWithIndex.map { case (timestamp, index) =>
      SnapshotMetadata(persistenceId, index + 1L, timestamp)
    }
    Future.traverse(metadata)(md => boundsDao.save(md, s"bounds-${md.sequenceNr}")).futureValue
    metadata
  }

  private def allSequenceNumbers(persistenceId: String): Future[Seq[Long]] = {
    import boundsDao.queries.profile.api._
    db.run(boundsDao.queries.selectAll(persistenceId).result).map(_.map(_.sequenceNumber))
  }

  private val boundsCases = Seq(
    ("default bounds", SnapshotSelectionCriteria.Latest, Seq(1L, 2L, 3L, 4L, 5L)),
    ("minimum sequence number", SnapshotSelectionCriteria(minSequenceNr = 3), Seq(3L, 4L, 5L)),
    ("unordered timestamps", SnapshotSelectionCriteria(maxSequenceNr = 4, minTimestamp = 350), Seq(2L)),
    ("all bounds", SnapshotSelectionCriteria(4, 400, 2, 300), Seq(2L, 4L)),
    ("equal bounds", SnapshotSelectionCriteria(3, 200, 3, 200), Seq(3L)),
    ("empty interval", SnapshotSelectionCriteria(maxSequenceNr = 2, minSequenceNr = 4), Seq.empty[Long]))

  "Snapshot selection bounds" must {
    boundsCases.foreach { case (label, criteria, matching) =>
      s"load with $label" in {
        val persistenceId = s"$pid-bounds"
        val metadata = seedBoundsSnapshots(persistenceId)
        val probe = TestProbe()
        snapshotStore.tell(LoadSnapshot(persistenceId, criteria, Long.MaxValue), probe.ref)
        val expected = matching.lastOption.map { sequenceNr =>
          SelectedSnapshot(metadata((sequenceNr - 1).toInt), s"bounds-$sequenceNr")
        }
        probe.expectMsg(LoadSnapshotResult(expected, Long.MaxValue))
      }

      s"delete with $label without deleting other snapshots" in {
        val persistenceId = s"$pid-bounds"
        val metadata = seedBoundsSnapshots(persistenceId)
        val otherMetadata = seedBoundsSnapshots(s"$persistenceId-other")
        val probe = TestProbe()
        snapshotStore.tell(DeleteSnapshots(persistenceId, criteria), probe.ref)
        probe.expectMsg(DeleteSnapshotsSuccess(criteria))
        val survivors = metadata.filterNot(md => matching.contains(md.sequenceNr))
        allSequenceNumbers(persistenceId).futureValue shouldBe survivors.map(_.sequenceNr).reverse
        allSequenceNumbers(otherMetadata.head.persistenceId).futureValue shouldBe
        otherMetadata.map(_.sequenceNr).reverse
      }
    }
  }

  override def beforeAll(): Unit = {
    dropAndCreate(schemaType)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    db.close()
    TestKit.shutdownActorSystem(system)
    super.afterAll()
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
