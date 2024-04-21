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
import pekko.persistence.jdbc.config.{ EventJournalTableConfiguration, EventTagTableConfiguration }
import pekko.persistence.jdbc.journal.dao.JournalTables.{ JournalPekkoSerializationRow, TagRow }
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

class JournalQueries(
    val profile: JdbcProfile,
    override val journalTableCfg: EventJournalTableConfiguration,
    override val tagTableCfg: EventTagTableConfiguration)
    extends JournalTables {

  import profile.api._

  private val JournalTableC = Compiled(JournalTable)
  private val insertAndReturn = JournalTable.returning(JournalTable.map(_.ordering))
  private val TagTableC = Compiled(TagTable)

  val highestSequenceNrForPersistenceId = Compiled(_highestSequenceNrForPersistenceId _)
  val highestSequenceNrForPersistenceIdBefore = Compiled(_highestSequenceNrForPersistenceIdBefore _)
  val messagesQuery = Compiled(_messagesQuery _)

  def writeJournalRows(xs: Seq[(JournalPekkoSerializationRow, Set[String])])(implicit ec: ExecutionContext) = {
    val sorted = xs.sortBy(event => event._1.sequenceNumber)
    if (sorted.exists(_._2.nonEmpty)) {
      // only if there are any tags
      val (events, tags) = sorted.unzip
      for {
        ids <- insertAndReturn ++= events
        tagInserts = ids.zip(tags).flatMap { case (id, tags) => tags.map(tag => TagRow(id, tag)) }
        _ <- TagTableC ++= tagInserts
      } yield ()
    } else {
      // optimization avoid some work when not using tags
      val events = sorted.map(_._1)
      JournalTableC ++= events
    }
  }

  private def selectAllJournalForPersistenceId(persistenceId: Rep[String]) =
    JournalTable.filter(_.persistenceId === persistenceId).sortBy(_.sequenceNumber.desc)

  def delete(persistenceId: String, toSequenceNr: Long) = {
    JournalTable.filter(_.persistenceId === persistenceId).filter(_.sequenceNumber <= toSequenceNr).delete
  }

  def markSeqNrJournalMessagesAsDeleted(persistenceId: String, sequenceNr: Long) =
    JournalTable
      .filter(_.persistenceId === persistenceId)
      .filter(_.sequenceNumber === sequenceNr)
      .filter(_.deleted === false)
      .map(_.deleted)
      .update(true)

  private def _highestSequenceNrForPersistenceId(persistenceId: Rep[String]): Rep[Option[Long]] =
    selectAllJournalForPersistenceId(persistenceId).take(1).map(_.sequenceNumber).max

  private def _highestSequenceNrForPersistenceIdBefore(
      persistenceId: Rep[String],
      maxSequenceNr: Rep[Long]): Query[Rep[Long], Long, Seq] =
    selectAllJournalForPersistenceId(persistenceId)
      .filter(_.sequenceNumber <= maxSequenceNr)
      .map(_.sequenceNumber)
      .take(1)

  private def _messagesQuery(
      persistenceId: Rep[String],
      fromSequenceNr: Rep[Long],
      toSequenceNr: Rep[Long],
      max: ConstColumn[Long]) =
    JournalTable
      .filter(_.persistenceId === persistenceId)
      .filter(_.deleted === false)
      .filter(_.sequenceNumber >= fromSequenceNr)
      .filter(_.sequenceNumber <= toSequenceNr) // TODO optimized this avoid large offset query.
      .sortBy(_.sequenceNumber.asc)
      .take(max)

}
