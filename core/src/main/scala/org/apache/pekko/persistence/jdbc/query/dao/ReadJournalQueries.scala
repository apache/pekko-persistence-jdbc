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

package org.apache.pekko.persistence.jdbc.query.dao

import org.apache.pekko
import pekko.persistence.jdbc.config.{ EventJournalTableConfiguration, EventTagTableConfiguration, ReadJournalConfig }
import pekko.persistence.jdbc.journal.dao.JournalTables
import slick.jdbc.JdbcProfile

class ReadJournalQueries(val profile: JdbcProfile, val readJournalConfig: ReadJournalConfig) extends JournalTables {
  override val journalTableCfg: EventJournalTableConfiguration = readJournalConfig.eventJournalTableConfiguration
  override def tagTableCfg: EventTagTableConfiguration = readJournalConfig.eventTagTableConfiguration

  import profile.api._

  def journalRowByPersistenceIds(persistenceIds: Iterable[String]) =
    for {
      query <- JournalTable.map(_.persistenceId)
      if query.inSetBind(persistenceIds)
    } yield query

  private def _allPersistenceIdsDistinct(max: ConstColumn[Long]): Query[Rep[String], String, Seq] =
    baseTableQuery().map(_.persistenceId).distinct.take(max)

  private def baseTableQuery() =
    JournalTable.filter(_.deleted === false)
  val allPersistenceIdsDistinct = Compiled(_allPersistenceIdsDistinct _)

  private def _messagesQuery(
      persistenceId: Rep[String],
      fromSequenceNr: Rep[Long],
      toSequenceNr: Rep[Long],
      max: ConstColumn[Long]) =
    baseTableQuery()
      .filter(_.persistenceId === persistenceId)
      .filter(_.sequenceNumber >= fromSequenceNr)
      .filter(_.sequenceNumber <= toSequenceNr)
      .filter(!_.deleted)
      .sortBy(_.sequenceNumber.asc)
      .take(max)

  val messagesQuery = Compiled(_messagesQuery _)

  private def _eventsByTag(
      tag: Rep[String],
      offset: ConstColumn[Long],
      maxOffset: ConstColumn[Long],
      max: ConstColumn[Long]) = {
    baseTableQuery()
      .filter(row => row.ordering > offset && row.ordering <= maxOffset)
      .sortBy(_.ordering.asc)
      .join(TagTable)
      .on(_.ordering === _.eventId)
      .filter(_._2.tag === tag)
      .take(max)
      .map(_._1)
  }

  val eventsByTag = Compiled(_eventsByTag _)

  private def _journalSequenceQuery(from: ConstColumn[Long], limit: ConstColumn[Long]) =
    JournalTable.filter(_.ordering > from).map(_.ordering).sorted.take(limit)

  val journalSequenceQuery = Compiled(_journalSequenceQuery _)

  val maxJournalSequenceQuery = Compiled {
    JournalTable.map(_.ordering).max.getOrElse(0L)
  }

}
