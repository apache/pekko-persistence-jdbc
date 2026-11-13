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
import pekko.persistence.jdbc.config.SnapshotConfig
import pekko.persistence.jdbc.db.{ SlickDatabase, SlickExtension }
import pekko.persistence.jdbc.migrator.SnapshotMigrator.{ FetchSize, NoParallelism, SnapshotStoreConfig }
import pekko.persistence.jdbc.snapshot.dao.DefaultSnapshotDao
import pekko.persistence.jdbc.snapshot.dao.legacy.{ ByteArraySnapshotSerializer, SnapshotQueries }
import pekko.persistence.jdbc.snapshot.dao.legacy.SnapshotTables.SnapshotRow
import pekko.serialization.{ Serialization, SerializationExtension }
import pekko.stream.scaladsl.{ Sink, Source }
import org.slf4j.{ Logger, LoggerFactory }
import slick.jdbc
import slick.jdbc.{ JdbcProfile, ResultSetConcurrency, ResultSetType }

import scala.concurrent.Future

/**
 * This will help migrate the legacy snapshot data onto the new snapshot schema with the
 * appropriate serialization
 *
 * The migrator opens its own database connection pool unless `use-shared-db` is configured. Call `close()` once
 * the `Future` returned by `migrateLatest()` or `migrateAll()` has
 * completed, to release that pool.
 *
 * @param system the actor system
 */
case class SnapshotMigrator(profile: JdbcProfile)(implicit system: ActorSystem) extends AutoCloseable {
  val log: Logger = LoggerFactory.getLogger(getClass)

  import system.dispatcher
  import profile.api._

  private val snapshotConfig: SnapshotConfig = new SnapshotConfig(system.settings.config.getConfig(SnapshotStoreConfig))

  private val snapshotSlickDb: SlickDatabase =
    SlickExtension(system).database(system.settings.config.getConfig(SnapshotStoreConfig))
  private[jdbc] val snapshotDB: jdbc.JdbcBackend.Database = snapshotSlickDb.database

  private val serialization: Serialization = SerializationExtension(system)
  private val queries: SnapshotQueries = new SnapshotQueries(profile, snapshotConfig.legacySnapshotTableConfiguration)
  private val serializer: ByteArraySnapshotSerializer = new ByteArraySnapshotSerializer(serialization)

  // get the instance if the default snapshot dao
  private val defaultSnapshotDao: DefaultSnapshotDao =
    new DefaultSnapshotDao(snapshotDB, profile, snapshotConfig, serialization)

  // stream the snapshots instead of loading them all into memory: some drivers, such as the PostgreSQL one, only
  // stream inside a transaction with a fetch size
  private val allSnapshotsQuery =
    queries.SnapshotTable.result
      .withStatementParameters(
        rsType = ResultSetType.ForwardOnly,
        rsConcurrency = ResultSetConcurrency.ReadOnly,
        fetchSize = FetchSize)
      .transactionally

  private def toSnapshotData(row: SnapshotRow): (SnapshotMetadata, Any) = serializer.deserialize(row).get

  /**
   * migrate the latest snapshot data
   */
  def migrateLatest(): Future[Done] = {
    // take the persistence ids from the legacy snapshot table rather than from the journal: an actor that deleted its
    // events after saving a snapshot has no undeleted journal rows left, and its snapshot must still be migrated
    Source
      .fromPublisher(snapshotDB.stream(queries.SnapshotTable.map(_.persistenceId).distinct.result))
      .mapAsync(NoParallelism) { persistenceId =>
        // let us fetch the latest snapshot for each persistenceId
        // wait for the save, so that a failure fails the migration and the returned Future only completes once every
        // snapshot has been written
        snapshotDB.run(queries.selectLatestByPersistenceId(persistenceId).result).flatMap { rows =>
          rows.headOption.map(toSnapshotData) match {
            case Some((metadata, value)) =>
              log.debug(s"migrating snapshot for ${metadata.toString}")
              defaultSnapshotDao.save(metadata, value)
            case None => Future.unit
          }
        }
      }
      .runWith(Sink.ignore)
  }

  /**
   * migrate all the legacy snapshot schema data into the new snapshot schema
   */
  def migrateAll(): Future[Done] = Source
    .fromPublisher(snapshotDB.stream(allSnapshotsQuery))
    .mapAsync(NoParallelism) { record =>
      val (metadata, value) = toSnapshotData(record)
      log.debug(s"migrating snapshot for ${metadata.toString}")
      defaultSnapshotDao.save(metadata, value)
    }
    .run()

  /**
   * Closes the database that was opened for this migrator. It is a no-op when the database is shared (configured with
   * `use-shared-db`), since that one is owned by the actor system.
   */
  override def close(): Unit =
    if (snapshotSlickDb.allowShutdown) snapshotDB.close()
}

case object SnapshotMigrator {
  final val SnapshotStoreConfig: String = "jdbc-snapshot-store"
  final val NoParallelism: Int = 1
  private final val FetchSize: Int = 100
}
