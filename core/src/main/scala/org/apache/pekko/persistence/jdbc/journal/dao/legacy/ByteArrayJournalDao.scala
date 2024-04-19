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

package org.apache.pekko.persistence.jdbc.journal.dao.legacy

import org.apache.pekko
import pekko.persistence.jdbc.config.{ BaseDaoConfig, JournalConfig }
import pekko.persistence.jdbc.journal.dao.{ BaseDao, BaseJournalDaoWithReadMessages, H2Compat, JournalDaoWithUpdates }
import pekko.persistence.jdbc.serialization.FlowPersistentReprSerializer
import pekko.persistence.{ AtomicWrite, PersistentRepr }
import pekko.serialization.Serialization
import pekko.stream.Materializer
import pekko.stream.scaladsl.Source
import pekko.{ Done, NotUsed }
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.collection.immutable.{ Nil, Seq }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class ByteArrayJournalDao(
    val db: Database,
    val profile: JdbcProfile,
    val journalConfig: JournalConfig,
    serialization: Serialization)(implicit val ec: ExecutionContext, val mat: Materializer)
    extends BaseByteArrayJournalDao {
  val queries = new JournalQueries(profile, journalConfig.journalTableConfiguration)
  val serializer = new ByteArrayJournalSerializer(serialization, journalConfig.pluginConfig.tagSeparator)
}

/**
 * The DefaultJournalDao contains all the knowledge to persist and load serialized journal entries
 */
trait BaseByteArrayJournalDao
    extends BaseDao[JournalRow]
    with JournalDaoWithUpdates
    with BaseJournalDaoWithReadMessages
    with H2Compat {
  val db: Database
  val profile: JdbcProfile
  val queries: JournalQueries
  val journalConfig: JournalConfig
  override def baseDaoConfig: BaseDaoConfig = journalConfig.daoConfig
  val serializer: FlowPersistentReprSerializer[JournalRow]
  implicit val ec: ExecutionContext
  implicit val mat: Materializer

  import profile.api._

  val logger = LoggerFactory.getLogger(this.getClass)

  def writeJournalRows(xs: Seq[JournalRow]): Future[Unit] = { // Write atomically without auto-commit
    db.run(queries.writeJournalRows(xs).transactionally).map(_ => ())
  }

  /**
   * @see [[pekko.persistence.journal.AsyncWriteJournal.asyncWriteMessages(messages)]]
   */
  def asyncWriteMessages(messages: Seq[AtomicWrite]): Future[Seq[Try[Unit]]] = {
    val serializedTries: Seq[Try[Seq[JournalRow]]] = serializer.serialize(messages)

    // If serialization fails for some AtomicWrites, the other AtomicWrites may still be written
    val rowsToWrite: Seq[JournalRow] = for {
      serializeTry <- serializedTries
      row <- serializeTry.getOrElse(Seq.empty)
    } yield row

    def resultWhenWriteComplete =
      if (serializedTries.forall(_.isSuccess)) Nil else serializedTries.map(_.map(_ => ()))

    queueWriteJournalRows(rowsToWrite).map(_ => resultWhenWriteComplete)
  }

  override def delete(persistenceId: String, maxSequenceNr: Long): Future[Unit] = {
    // We should keep journal record with highest sequence number in order to be compliant
    // with @see [[pekko.persistence.journal.JournalSpec]]
    db.run(queries.delete(persistenceId, maxSequenceNr)).map(_ => ())
  }

  def update(persistenceId: String, sequenceNr: Long, payload: AnyRef): Future[Done] = {
    val write = PersistentRepr(payload, sequenceNr, persistenceId)
    val serializedRow = serializer.serialize(write) match {
      case Success(t) => t
      case Failure(cause) =>
        throw new IllegalArgumentException(
          s"Failed to serialize ${write.getClass} for update of [$persistenceId] @ [$sequenceNr]",
          cause)
    }
    db.run(queries.update(persistenceId, sequenceNr, serializedRow.message).map(_ => Done))
  }

  override def highestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] =
    for {
      maybeHighestSeqNo <- db.run(queries.highestSequenceNrForPersistenceId(persistenceId).result)
    } yield maybeHighestSeqNo.getOrElse(0L)

  override def messages(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      max: Long): Source[Try[(PersistentRepr, Long)], NotUsed] =
    Source
      .fromPublisher(
        db.stream(
          queries.messagesQuery((persistenceId, fromSequenceNr, toSequenceNr, correctMaxForH2Driver(max))).result))
      .via(serializer.deserializeFlow)
      .map {
        case Success((repr, _, ordering)) => Success(repr -> ordering)
        case Failure(e)                   => Failure(e)
      }
}
