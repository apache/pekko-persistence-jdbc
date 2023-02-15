/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.query.dao
import org.apache.pekko.NotUsed
import org.apache.pekko.persistence.PersistentRepr
import org.apache.pekko.persistence.jdbc.PekkoSerialization
import org.apache.pekko.persistence.jdbc.config.ReadJournalConfig
import org.apache.pekko.persistence.jdbc.journal.dao.{ BaseJournalDaoWithReadMessages, H2Compat }
import org.apache.pekko.serialization.Serialization
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

class DefaultReadJournalDao(
    val db: Database,
    val profile: JdbcProfile,
    val readJournalConfig: ReadJournalConfig,
    serialization: Serialization)(implicit val ec: ExecutionContext, val mat: Materializer)
    extends ReadJournalDao
    with BaseJournalDaoWithReadMessages
    with H2Compat {
  import profile.api._

  val queries = new ReadJournalQueries(profile, readJournalConfig)

  override def allPersistenceIdsSource(max: Long): Source[String, NotUsed] =
    Source.fromPublisher(db.stream(queries.allPersistenceIdsDistinct(correctMaxForH2Driver(max)).result))

  override def eventsByTag(
      tag: String,
      offset: Long,
      maxOffset: Long,
      max: Long): Source[Try[(PersistentRepr, Set[String], Long)], NotUsed] = {

    // This doesn't populate the tags. AFAICT they aren't used
    Source
      .fromPublisher(db.stream(queries.eventsByTag((tag, offset, maxOffset, correctMaxForH2Driver(max))).result))
      .map(row =>
        PekkoSerialization.fromRow(serialization)(row).map { case (repr, ordering) => (repr, Set.empty, ordering) })
  }

  override def journalSequence(offset: Long, limit: Long): Source[Long, NotUsed] =
    Source.fromPublisher(db.stream(queries.journalSequenceQuery((offset, limit)).result))

  override def maxJournalSequence(): Future[Long] =
    db.run(queries.maxJournalSequenceQuery.result)

  override def messages(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      max: Long): Source[Try[(PersistentRepr, Long)], NotUsed] =
    Source
      .fromPublisher(
        db.stream(
          queries.messagesQuery((persistenceId, fromSequenceNr, toSequenceNr, correctMaxForH2Driver(max))).result))
      .map(PekkoSerialization.fromRow(serialization)(_))

}
