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

import com.typesafe.config.{ Config, ConfigFactory }
import org.apache.pekko.persistence.jdbc.config.{ JournalConfig, ReadJournalConfig, SlickConfiguration, SnapshotConfig }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class PekkoPersistenceConfigTest extends AnyFlatSpec with Matchers {
  private val referenceConfig: Config = ConfigFactory.load("reference")

  val config: Config = ConfigFactory
    .parseString("""
          |jdbc-journal {
          |  tables {
          |    event_journal {
          |      tableName = "custom_event_journal"
          |      schemaName = "custom"
          |      columnNames {
          |        ordering = "custom_ordering"
          |        persistenceId = "custom_persistence_id"
          |      }
          |    }
          |    event_tag {
          |      tableName = "custom_event_tag"
          |      legacy-tag-key = false
          |    }
          |  }
          |
          |  dao = "com.example.CustomJournalDao"
          |}
          |
          |jdbc-snapshot-store {
          |  tables {
          |    snapshot {
          |      tableName = "custom_snapshot"
          |      schemaName = "custom"
          |      columnNames {
          |        snapshotPayload = "custom_snapshot_payload"
          |      }
          |    }
          |  }
          |
          |  dao = "com.example.CustomSnapshotDao"
          |}
          |
          |jdbc-read-journal {
          |  refresh-interval = "300ms"
          |  max-buffer-size = "10"
          |
          |  dao = "com.example.CustomReadJournalDao"
          |
          |  tables {
          |    event_journal {
          |      tableName = "custom_event_journal"
          |      schemaName = "custom"
          |    }
          |  }
          |}
    """.stripMargin)
    .withFallback(referenceConfig)
    .resolve()

  "reference config" should "parse JournalConfig" in {
    val cfg = new JournalConfig(referenceConfig.getConfig("jdbc-journal"))
    val slickConfiguration = new SlickConfiguration(referenceConfig.getConfig("jdbc-journal.slick"))
    slickConfiguration.jndiName shouldBe None
    slickConfiguration.jndiDbName shouldBe None

    cfg.pluginConfig.dao shouldBe "org.apache.pekko.persistence.jdbc.journal.dao.DefaultJournalDao"

    cfg.eventJournalTableConfiguration.tableName shouldBe "event_journal"
    cfg.eventJournalTableConfiguration.schemaName shouldBe None

    cfg.eventJournalTableConfiguration.columnNames.ordering shouldBe "ordering"
    cfg.eventJournalTableConfiguration.columnNames.deleted shouldBe "deleted"
    cfg.eventJournalTableConfiguration.columnNames.persistenceId shouldBe "persistence_id"
    cfg.eventJournalTableConfiguration.columnNames.sequenceNumber shouldBe "sequence_number"
    cfg.eventJournalTableConfiguration.columnNames.eventPayload shouldBe "event_payload"

    cfg.eventTagTableConfiguration.tableName shouldBe "event_tag"
    cfg.eventTagTableConfiguration.schemaName shouldBe None
    cfg.eventTagTableConfiguration.legacyTagKey shouldBe true
    cfg.eventTagTableConfiguration.columnNames.tag shouldBe "tag"
  }

  it should "parse SnapshotConfig" in {
    val cfg = new SnapshotConfig(referenceConfig.getConfig("jdbc-snapshot-store"))
    val slickConfiguration = new SlickConfiguration(referenceConfig.getConfig("jdbc-journal.slick"))
    slickConfiguration.jndiName shouldBe None
    slickConfiguration.jndiDbName shouldBe None

    cfg.pluginConfig.dao shouldBe "org.apache.pekko.persistence.jdbc.snapshot.dao.DefaultSnapshotDao"

    cfg.snapshotTableConfiguration.tableName shouldBe "snapshot"
    cfg.snapshotTableConfiguration.schemaName shouldBe None

    cfg.snapshotTableConfiguration.columnNames.persistenceId shouldBe "persistence_id"
    cfg.snapshotTableConfiguration.columnNames.created shouldBe "created"
    cfg.snapshotTableConfiguration.columnNames.sequenceNumber shouldBe "sequence_number"
    cfg.snapshotTableConfiguration.columnNames.snapshotPayload shouldBe "snapshot_payload"
    cfg.snapshotTableConfiguration.columnNames.metaPayload shouldBe "meta_payload"
  }

  it should "parse ReadJournalConfig" in {
    val cfg = new ReadJournalConfig(referenceConfig.getConfig("jdbc-read-journal"))
    val slickConfiguration = new SlickConfiguration(referenceConfig.getConfig("jdbc-journal.slick"))
    slickConfiguration.jndiName shouldBe None
    slickConfiguration.jndiDbName shouldBe None

    cfg.pluginConfig.dao shouldBe "org.apache.pekko.persistence.jdbc.query.dao.DefaultReadJournalDao"
    cfg.refreshInterval shouldBe 1.second
    cfg.maxBufferSize shouldBe 500

    cfg.eventJournalTableConfiguration.tableName shouldBe "event_journal"
    cfg.eventJournalTableConfiguration.schemaName shouldBe None
    cfg.eventJournalTableConfiguration.columnNames.ordering shouldBe "ordering"
    cfg.eventJournalTableConfiguration.columnNames.persistenceId shouldBe "persistence_id"
    cfg.eventJournalTableConfiguration.columnNames.sequenceNumber shouldBe "sequence_number"

    cfg.eventTagTableConfiguration.tableName shouldBe "event_tag"
  }

  "full config" should "parse JournalConfig" in {
    val cfg = new JournalConfig(config.getConfig("jdbc-journal"))

    cfg.pluginConfig.dao shouldBe "com.example.CustomJournalDao"

    cfg.eventJournalTableConfiguration.tableName shouldBe "custom_event_journal"
    cfg.eventJournalTableConfiguration.schemaName shouldBe Some("custom")

    // overridden column names
    cfg.eventJournalTableConfiguration.columnNames.ordering shouldBe "custom_ordering"
    cfg.eventJournalTableConfiguration.columnNames.persistenceId shouldBe "custom_persistence_id"
    // column names that fall back to the reference config
    cfg.eventJournalTableConfiguration.columnNames.deleted shouldBe "deleted"
    cfg.eventJournalTableConfiguration.columnNames.sequenceNumber shouldBe "sequence_number"

    cfg.eventTagTableConfiguration.tableName shouldBe "custom_event_tag"
    cfg.eventTagTableConfiguration.legacyTagKey shouldBe false
    cfg.eventTagTableConfiguration.columnNames.tag shouldBe "tag"
  }

  it should "parse SnapshotConfig" in {
    val cfg = new SnapshotConfig(config.getConfig("jdbc-snapshot-store"))

    cfg.pluginConfig.dao shouldBe "com.example.CustomSnapshotDao"

    cfg.snapshotTableConfiguration.tableName shouldBe "custom_snapshot"
    cfg.snapshotTableConfiguration.schemaName shouldBe Some("custom")
    cfg.snapshotTableConfiguration.columnNames.snapshotPayload shouldBe "custom_snapshot_payload"
    cfg.snapshotTableConfiguration.columnNames.persistenceId shouldBe "persistence_id"
    cfg.snapshotTableConfiguration.columnNames.created shouldBe "created"
    cfg.snapshotTableConfiguration.columnNames.sequenceNumber shouldBe "sequence_number"
  }

  it should "parse ReadJournalConfig" in {
    val cfg = new ReadJournalConfig(config.getConfig("jdbc-read-journal"))

    cfg.pluginConfig.dao shouldBe "com.example.CustomReadJournalDao"
    cfg.refreshInterval shouldBe 300.millis
    cfg.maxBufferSize shouldBe 10

    cfg.eventJournalTableConfiguration.tableName shouldBe "custom_event_journal"
    cfg.eventJournalTableConfiguration.schemaName shouldBe Some("custom")
    cfg.eventJournalTableConfiguration.columnNames.ordering shouldBe "ordering"
    cfg.eventJournalTableConfiguration.columnNames.persistenceId shouldBe "persistence_id"
  }
}
