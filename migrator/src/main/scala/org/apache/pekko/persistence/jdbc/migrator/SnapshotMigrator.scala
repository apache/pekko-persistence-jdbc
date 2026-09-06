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

package org.apache.pekko.persistence.jdbc.migrator

import org.apache.pekko
import pekko.Done
import pekko.actor.ActorSystem
import pekko.persistence.SnapshotMetadata
import pekko.persistence.jdbc.config.{ ReadJournalConfig, SnapshotConfig }
import pekko.persistence.jdbc.db.{ SlickDatabase, SlickExtension }
import pekko.persistence.jdbc.migrator.SnapshotMigrator.{ NoParallelism, SnapshotStoreConfig }
import pekko.persistence.jdbc.query.dao.legacy.ByteArrayReadJournalDao
import pekko.persistence.jdbc.snapshot.dao.DefaultSnapshotDao
import pekko.persistence.jdbc.snapshot.dao.legacy.{ ByteArraySnapshotSerializer, SnapshotQueries }
import pekko.persistence.jdbc.snapshot.dao.legacy.SnapshotTables.SnapshotRow
import pekko.serialization.{ Serialization, SerializationExtension }
import pekko.stream.scaladsl.{ Sink, Source }
import org.slf4j.{ Logger, LoggerFactory }
import slick.jdbc
import slick.jdbc.{ JdbcBackend, JdbcProfile }

import scala.concurrent.Future

/**
 * This will help migrate the legacy snapshot data onto the new snapshot schema with the
 * appropriate serialization
 *
 * @param system the actor system
 */
case class SnapshotMigrator(profile: JdbcProfile)(implicit system: ActorSystem) extends AutoCloseable {
  val log: Logger = LoggerFactory.getLogger(getClass)

  import system.dispatcher
  import profile.api._

  private val snapshotConfig: SnapshotConfig = new SnapshotConfig(system.settings.config.getConfig(SnapshotStoreConfig))
  private val readJournalConfig: ReadJournalConfig = new ReadJournalConfig(
    system.settings.config.getConfig(JournalMigrator.ReadJournalConfig))

  private val snapshotSlickDb: SlickDatabase =
    SlickExtension(system).database(system.settings.config.getConfig(SnapshotStoreConfig))
  private[jdbc] val snapshotDB: jdbc.JdbcBackend.Database = snapshotSlickDb.database

  private val journalSlickDb: SlickDatabase =
    SlickExtension(system).database(system.settings.config.getConfig(JournalMigrator.ReadJournalConfig))
  private[jdbc] val journalDB: JdbcBackend.Database = journalSlickDb.database

  private val serialization: Serialization = SerializationExtension(system)
  private val queries: SnapshotQueries = new SnapshotQueries(profile, snapshotConfig.legacySnapshotTableConfiguration)
  private val serializer: ByteArraySnapshotSerializer = new ByteArraySnapshotSerializer(serialization)

  // get the instance if the default snapshot dao
  private val defaultSnapshotDao: DefaultSnapshotDao =
    new DefaultSnapshotDao(snapshotDB, profile, snapshotConfig, serialization)

  // get the instance of the legacy journal DAO
  private val legacyJournalDao: ByteArrayReadJournalDao =
    new ByteArrayReadJournalDao(journalDB, profile, readJournalConfig, SerializationExtension(system))

  private def toSnapshotData(row: SnapshotRow): (SnapshotMetadata, Any) = serializer.deserialize(row).get

  /**
   * migrate the latest snapshot data
   */
  def migrateLatest(): Future[Done] = {
    legacyJournalDao
      .allPersistenceIdsSource(Long.MaxValue)
      .mapAsync(NoParallelism) { persistenceId =>
        // let us fetch the latest snapshot for each persistenceId
        snapshotDB.run(queries.selectLatestByPersistenceId(persistenceId).result).map { rows =>
          rows.headOption.map(toSnapshotData).map { case (metadata, value) =>
            log.debug(s"migrating snapshot for ${metadata.toString}")
            defaultSnapshotDao.save(metadata, value)
          }
        }
      }
      .runWith(Sink.ignore)
  }

  /**
   * migrate all the legacy snapshot schema data into the new snapshot schema
   */
  def migrateAll(): Future[Done] = Source
    .fromPublisher(snapshotDB.stream(queries.SnapshotTable.result))
    .mapAsync(NoParallelism) { record =>
      val (metadata, value) = toSnapshotData(record)
      log.debug(s"migrating snapshot for ${metadata.toString}")
      defaultSnapshotDao.save(metadata, value)
    }
    .run()

  /**
   * Closes the databases that were opened for this migrator. Databases that are shared (configured with
   * `use-shared-db`) are left alone, since those are owned by the actor system.
   */
  override def close(): Unit = {
    if (snapshotSlickDb.allowShutdown) snapshotDB.close()
    if (journalSlickDb.allowShutdown) journalDB.close()
  }
}

case object SnapshotMigrator {
  final val SnapshotStoreConfig: String = "jdbc-snapshot-store"
  final val NoParallelism: Int = 1
}
