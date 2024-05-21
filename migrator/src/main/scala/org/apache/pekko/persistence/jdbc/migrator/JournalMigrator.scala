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
import pekko.persistence.PersistentRepr
import pekko.persistence.jdbc.PekkoSerialization
import pekko.persistence.jdbc.config.{ JournalConfig, ReadJournalConfig }
import pekko.persistence.jdbc.db.SlickExtension
import pekko.persistence.jdbc.journal.dao.JournalQueries
import pekko.persistence.jdbc.journal.dao.legacy.ByteArrayJournalSerializer
import pekko.persistence.jdbc.journal.dao.JournalTables.{ JournalPekkoSerializationRow, TagRow }
import pekko.persistence.jdbc.query.dao.legacy.ReadJournalQueries
import pekko.serialization.{ Serialization, SerializationExtension }
import pekko.stream.scaladsl.Source
import org.slf4j.{ Logger, LoggerFactory }
import slick.jdbc._

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }

/**
 * This will help migrate the legacy journal data onto the new journal schema with the
 * appropriate serialization
 *
 * @param system the actor system
 */
final case class JournalMigrator(profile: JdbcProfile)(implicit system: ActorSystem) {
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  import profile.api._

  val log: Logger = LoggerFactory.getLogger(getClass)

  // get the various configurations
  private val journalConfig: JournalConfig =
    new JournalConfig(system.settings.config.getConfig(JournalMigrator.JournalConfig))
  private val readJournalConfig: ReadJournalConfig = new ReadJournalConfig(
    system.settings.config.getConfig(JournalMigrator.ReadJournalConfig))

  // the journal database
  private val journalDB: JdbcBackend.Database =
    SlickExtension(system).database(system.settings.config.getConfig(JournalMigrator.ReadJournalConfig)).database

  // get an instance of the new journal queries
  private val newJournalQueries: JournalQueries =
    new JournalQueries(profile, journalConfig.eventJournalTableConfiguration, journalConfig.eventTagTableConfiguration)

  // let us get the journal reader
  private val serialization: Serialization = SerializationExtension(system)
  private val legacyJournalQueries: ReadJournalQueries = new ReadJournalQueries(profile, readJournalConfig)
  private val serializer: ByteArrayJournalSerializer =
    new ByteArrayJournalSerializer(serialization, readJournalConfig.pluginConfig.tagSeparator)

  private val bufferSize: Int = journalConfig.daoConfig.bufferSize

  private val query =
    legacyJournalQueries.JournalTable.result
      .withStatementParameters(
        rsType = ResultSetType.ForwardOnly,
        rsConcurrency = ResultSetConcurrency.ReadOnly,
        fetchSize = bufferSize)
      .transactionally

  /**
   * write all legacy events into the new journal tables applying the proper serialization
   */
  def migrate(): Future[Done] = Source
    .fromPublisher(journalDB.stream(query))
    .via(serializer.deserializeFlow)
    .map {
      case Success((repr, tags, ordering)) => (repr, tags, ordering)
      case Failure(exception)              => throw exception // blow-up on failure
    }
    .map { case (repr, tags, ordering) => serialize(repr, tags, ordering) }
    // get pages of many records at once
    .grouped(bufferSize)
    .mapAsync(1)(records => {
      val stmt: DBIO[Unit] = records
        // get all the sql statements for this record as an option
        .map { case (newRepr, newTags) =>
          log.debug(s"migrating event for PersistenceID: ${newRepr.persistenceId} with tags ${newTags.mkString(",")}")
          writeJournalRowsStatements(newRepr, newTags)
        }
        // reduce to 1 statement
        .foldLeft[DBIO[Unit]](DBIO.successful[Unit] {})((priorStmt, nextStmt) => {
          priorStmt.andThen(nextStmt)
        })

      journalDB.run(stmt)
    })
    .run()

  /**
   * serialize the PersistentRepr and construct a JournalPekkoSerializationRow and set of matching tags
   *
   * @param repr the PersistentRepr
   * @param tags the tags
   * @param ordering the ordering of the PersistentRepr
   * @return the tuple of JournalPekkoSerializationRow and set of tags
   */
  private def serialize(
      repr: PersistentRepr,
      tags: Set[String],
      ordering: Long): (JournalPekkoSerializationRow, Set[String]) = {

    val serializedPayload: PekkoSerialization.PekkoSerialized =
      PekkoSerialization.serialize(serialization, repr.payload).get

    val serializedMetadata: Option[PekkoSerialization.PekkoSerialized] =
      repr.metadata.flatMap(m => PekkoSerialization.serialize(serialization, m).toOption)
    val row: JournalPekkoSerializationRow = JournalPekkoSerializationRow(
      ordering,
      repr.deleted,
      repr.persistenceId,
      repr.sequenceNr,
      repr.writerUuid,
      repr.timestamp,
      repr.manifest,
      serializedPayload.payload,
      serializedPayload.serId,
      serializedPayload.serManifest,
      serializedMetadata.map(_.payload),
      serializedMetadata.map(_.serId),
      serializedMetadata.map(_.serManifest))

    (row, tags)
  }

  private def writeJournalRowsStatements(
      journalSerializedRow: JournalPekkoSerializationRow,
      tags: Set[String]): DBIO[Unit] = {
    for {
      id <- newJournalQueries.JournalTable
        .returning(newJournalQueries.JournalTable.map(_.ordering)) += journalSerializedRow
      tagInserts = tags.map(tag => TagRow(id, tag))
      _ <- newJournalQueries.TagTable ++= tagInserts
    } yield ()
  }
}

case object JournalMigrator {
  final val JournalConfig: String = "jdbc-journal"
  final val ReadJournalConfig: String = "jdbc-read-journal"
}
