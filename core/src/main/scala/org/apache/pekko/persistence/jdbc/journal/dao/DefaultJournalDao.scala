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

package org.apache.pekko.persistence.jdbc.journal.dao

import org.apache.pekko
import pekko.NotUsed
import pekko.dispatch.ExecutionContexts
import pekko.persistence.jdbc.PekkoSerialization
import pekko.persistence.jdbc.config.{ BaseDaoConfig, JournalConfig }
import pekko.persistence.jdbc.journal.dao.JournalTables.JournalPekkoSerializationRow
import pekko.persistence.journal.Tagged
import pekko.persistence.{ AtomicWrite, PersistentRepr }
import pekko.serialization.Serialization
import pekko.stream.Materializer
import pekko.stream.scaladsl.Source
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.collection.immutable
import scala.collection.immutable.{ Nil, Seq }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

/**
 * A [[JournalDao]] that uses Apache Pekko serialization to serialize the payload and store
 * the manifest and serializer id used.
 */
class DefaultJournalDao(
    val db: Database,
    val profile: JdbcProfile,
    val journalConfig: JournalConfig,
    serialization: Serialization)(implicit val ec: ExecutionContext, val mat: Materializer)
    extends BaseDao[(JournalPekkoSerializationRow, Set[String])]
    with BaseJournalDaoWithReadMessages
    with JournalDao
    with H2Compat {

  import profile.api._

  override def baseDaoConfig: BaseDaoConfig = journalConfig.daoConfig

  override def writeJournalRows(xs: immutable.Seq[(JournalPekkoSerializationRow, Set[String])]): Future[Unit] = {
    db.run(queries.writeJournalRows(xs).transactionally).map(_ => ())(ExecutionContexts.parasitic)
  }

  val queries =
    new JournalQueries(profile, journalConfig.eventJournalTableConfiguration, journalConfig.eventTagTableConfiguration)

  override def delete(persistenceId: String, maxSequenceNr: Long): Future[Unit] = {
    // We should keep journal record with highest sequence number in order to be compliant
    // with @see [[pekko.persistence.journal.JournalSpec]]
    val actions: DBIOAction[Unit, NoStream, Effect.Write with Effect.Read] = for {
      highestSequenceNr <- queries.highestSequenceNrForPersistenceIdBefore((persistenceId, maxSequenceNr)).result
      _ <- queries.delete(persistenceId, highestSequenceNr - 1)
      _ <- queries.markSeqNrJournalMessagesAsDeleted(persistenceId, highestSequenceNr)
    } yield ()

    db.run(actions.transactionally)
  }

  override def highestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = {
    for {
      maybeHighestSeqNo <- db.run(queries.highestSequenceNrForPersistenceId(persistenceId).result)
    } yield maybeHighestSeqNo.getOrElse(0L)
  }

  override def asyncWriteMessages(messages: immutable.Seq[AtomicWrite]): Future[immutable.Seq[Try[Unit]]] = {

    def serializeAtomicWrite(aw: AtomicWrite): Try[Seq[(JournalPekkoSerializationRow, Set[String])]] = {
      Try(aw.payload.map(serialize))
    }

    def serialize(pr: PersistentRepr): (JournalPekkoSerializationRow, Set[String]) = {

      val (updatedPr, tags) = pr.payload match {
        case Tagged(payload, tags) => (pr.withPayload(payload), tags)
        case _                     => (pr, Set.empty[String])
      }

      val serializedPayload = PekkoSerialization.serialize(serialization, updatedPr.payload).get
      val serializedMetadata = updatedPr.metadata.flatMap(m => PekkoSerialization.serialize(serialization, m).toOption)
      val row = JournalPekkoSerializationRow(
        Long.MinValue,
        updatedPr.deleted,
        updatedPr.persistenceId,
        updatedPr.sequenceNr,
        updatedPr.writerUuid,
        updatedPr.timestamp,
        updatedPr.manifest,
        serializedPayload.payload,
        serializedPayload.serId,
        serializedPayload.serManifest,
        serializedMetadata.map(_.payload),
        serializedMetadata.map(_.serId),
        serializedMetadata.map(_.serManifest))

      (row, tags)
    }

    val serializedTries = messages.map(serializeAtomicWrite)

    val rowsToWrite: Seq[(JournalPekkoSerializationRow, Set[String])] = for {
      serializeTry <- serializedTries
      row <- serializeTry.getOrElse(Seq.empty)
    } yield row

    def resultWhenWriteComplete =
      if (serializedTries.forall(_.isSuccess)) Nil else serializedTries.map(_.map(_ => ()))

    queueWriteJournalRows(rowsToWrite).map(_ => resultWhenWriteComplete)
  }

  override def messages(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      max: Long): Source[Try[(PersistentRepr, Long)], NotUsed] = {
    Source
      .fromPublisher(
        db.stream(
          queries.messagesQuery((persistenceId, fromSequenceNr, toSequenceNr, correctMaxForH2Driver(max))).result))
      .map(PekkoSerialization.fromRow(serialization)(_))
  }
}
