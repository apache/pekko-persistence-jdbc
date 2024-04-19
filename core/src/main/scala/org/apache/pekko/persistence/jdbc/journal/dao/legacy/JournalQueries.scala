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

package org.apache.pekko.persistence.jdbc
package journal.dao.legacy

import org.apache.pekko.persistence.jdbc.config.LegacyJournalTableConfiguration
import slick.jdbc.JdbcProfile

class JournalQueries(val profile: JdbcProfile, override val journalTableCfg: LegacyJournalTableConfiguration)
    extends JournalTables {
  import profile.api._

  private val JournalTableC = Compiled(JournalTable)

  def writeJournalRows(xs: Seq[JournalRow]) =
    JournalTableC ++= xs.sortBy(_.sequenceNumber)

  def delete(persistenceId: String, toSequenceNr: Long) = {
    JournalTable.filter(_.persistenceId === persistenceId).filter(_.sequenceNumber <= toSequenceNr).delete
  }

  /**
   * Updates (!) a payload stored in a specific events row.
   * Intended to be used sparingly, e.g. moving all events to their encrypted counterparts.
   */
  def update(persistenceId: String, seqNr: Long, replacement: Array[Byte]) = {
    val baseQuery = JournalTable.filter(_.persistenceId === persistenceId).filter(_.sequenceNumber === seqNr)

    baseQuery.map(_.message).update(replacement)
  }

  private def _highestSequenceNrForPersistenceId(persistenceId: Rep[String]): Rep[Option[Long]] =
    JournalTable.filter(_.persistenceId === persistenceId).take(1).map(_.sequenceNumber).max

  val highestSequenceNrForPersistenceId = Compiled(_highestSequenceNrForPersistenceId _)

  private def _messagesQuery(
      persistenceId: Rep[String],
      fromSequenceNr: Rep[Long],
      toSequenceNr: Rep[Long],
      max: ConstColumn[Long]) =
    JournalTable
      .filter(_.persistenceId === persistenceId)
      .filter(_.sequenceNumber >= fromSequenceNr)
      .filter(_.sequenceNumber <= toSequenceNr)
      .sortBy(_.sequenceNumber.asc)
      .take(max)

  val messagesQuery = Compiled(_messagesQuery _)

}
