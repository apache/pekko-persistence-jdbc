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

package org.apache.pekko.persistence.jdbc.query.dao.legacy

import org.apache.pekko
import pekko.persistence.jdbc.config.{ LegacyJournalTableConfiguration, ReadJournalConfig }
import pekko.persistence.jdbc.journal.dao.legacy.JournalTables
import slick.jdbc.JdbcProfile

class ReadJournalQueries(val profile: JdbcProfile, val readJournalConfig: ReadJournalConfig) extends JournalTables {
  override val journalTableCfg: LegacyJournalTableConfiguration = readJournalConfig.journalTableConfiguration

  import profile.api._

  val allPersistenceIdsDistinct = Compiled(_allPersistenceIdsDistinct _)
  val messagesQuery = Compiled(_messagesQuery _)
  val eventsByTag = Compiled(_eventsByTag _)
  val journalSequenceQuery = Compiled(_journalSequenceQuery _)
  val lastPersistenceIdSequenceNumberQuery = Compiled {
    _lastPersistenceIdSequenceNumberQuery _
  }
  val maxJournalSequenceQuery = Compiled {
    JournalTable.map(_.ordering).max.getOrElse(0L)
  }

  private def baseTableQuery() =
    JournalTable.filter(_.deleted === false)

  private def _allPersistenceIdsDistinct(max: ConstColumn[Long]): Query[Rep[String], String, Seq] =
    baseTableQuery().map(_.persistenceId).distinct.take(max)

  private def _lastPersistenceIdSequenceNumberQuery(
      persistenceId: Rep[String]
  ) = {

    baseTableQuery()
      .filter(_.persistenceId === persistenceId)
      .map(_.sequenceNumber)
      .max
  }

  private def _messagesQuery(
      persistenceId: Rep[String],
      fromSequenceNr: Rep[Long],
      toSequenceNr: Rep[Long],
      max: ConstColumn[Long]) =
    baseTableQuery()
      .filter(_.persistenceId === persistenceId)
      .filter(_.sequenceNumber >= fromSequenceNr)
      .filter(_.sequenceNumber <= toSequenceNr) // TODO perf: optimized this avoid large offset query.
      .sortBy(_.sequenceNumber.asc)
      .take(max)

  private def _eventsByTag(
      tag: Rep[String],
      offset: ConstColumn[Long],
      maxOffset: ConstColumn[Long],
      max: ConstColumn[Long]) = {
    baseTableQuery()
      .filter(_.tags.like(tag))
      .sortBy(_.ordering.asc)
      .filter(row => row.ordering > offset && row.ordering <= maxOffset)
      .take(max)
  }

  private def _journalSequenceQuery(from: ConstColumn[Long], limit: ConstColumn[Long]) =
    JournalTable.filter(_.ordering > from).map(_.ordering).sorted.take(limit)
}
