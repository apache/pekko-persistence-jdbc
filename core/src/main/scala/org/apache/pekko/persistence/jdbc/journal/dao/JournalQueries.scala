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

  def delete(persistenceId: String, toSequenceNr: Long) = {
    selectByPersistenceIdAndMaxSequenceNr(persistenceId, toSequenceNr).delete
  }

  private def _selectAllJournalForPersistenceId(persistenceId: Rep[String]) =
    _selectByPersistenceId(persistenceId).sortBy(_.sequenceNumber.desc)

  private def _selectByPersistenceId(persistenceId: Rep[String]) =
    JournalTable.filter(_.persistenceId === persistenceId)

  private def _selectByPersistenceIdAndMaxSequenceNr(persistenceId: Rep[String], maxSeqNr: Rep[Long]) =
    _selectByPersistenceId(persistenceId).filter(_.sequenceNumber <= maxSeqNr)
  val selectByPersistenceIdAndMaxSequenceNr = Compiled(_selectByPersistenceIdAndMaxSequenceNr _)

  private def _highestSequenceNrForPersistenceId(persistenceId: Rep[String]): Rep[Option[Long]] =
    _selectAllJournalForPersistenceId(persistenceId).take(1).map(_.sequenceNumber).max

  val highestSequenceNrForPersistenceId = Compiled(_highestSequenceNrForPersistenceId _)

  private def _allPersistenceIdsDistinct: Query[Rep[String], String, Seq] =
    JournalTable.map(_.persistenceId).distinct

  val allPersistenceIdsDistinct = Compiled(_allPersistenceIdsDistinct)

  private def _messagesQuery(
      persistenceId: Rep[String],
      fromSequenceNr: Rep[Long],
      toSequenceNr: Rep[Long],
      max: ConstColumn[Long]) =
    JournalTable
      .filter(_.persistenceId === persistenceId)
      .filter(_.sequenceNumber >= fromSequenceNr)
      .filter(_.sequenceNumber <= toSequenceNr) // TODO optimized this avoid large offset query.
      .sortBy(_.sequenceNumber.asc)
      .take(max)

  val messagesQuery = Compiled(_messagesQuery _)

}
