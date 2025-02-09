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
import pekko.NotUsed
import pekko.persistence.jdbc.query.dao.TestProbeReadJournalDao.JournalSequence
import pekko.persistence.PersistentRepr
import pekko.stream.scaladsl.Source
import pekko.testkit.TestProbe
import pekko.util.Timeout
import pekko.pattern.ask

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try
import pekko.actor.Scheduler

object TestProbeReadJournalDao {
  case class JournalSequence(offset: Long, limit: Long)
}

/**
 * Read journal dao where the journalSequence query is backed by a testprobe
 */
class TestProbeReadJournalDao(val probe: TestProbe) extends ReadJournalDao {
  // Since the testprobe is instrumented by the test, it should respond very fast
  implicit val askTimeout: Timeout = Timeout(100.millis)

  /**
   * Returns distinct stream of persistenceIds
   */
  override def allPersistenceIdsSource(max: Long): Source[String, NotUsed] = ???

  /**
   * Returns a Source of bytes for certain tag from an offset. The result is sorted by
   * created time asc thus the offset is relative to the creation time
   */
  override def eventsByTag(
      tag: String,
      offset: Long,
      maxOffset: Long,
      max: Long): Source[Try[(PersistentRepr, Set[String], Long)], NotUsed] = ???

  override def lastPersistenceIdSequenceNumber(
    persistenceId: String
  ): Future[Option[Long]] = ???

  /**
   * Returns a Source of bytes for a certain persistenceId
   */
  override def messages(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      max: Long): Source[Try[(PersistentRepr, Long)], NotUsed] = ???

  override def messagesWithBatch(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      batchSize: Int,
      refreshInterval: Option[(FiniteDuration, Scheduler)]): Source[Try[(PersistentRepr, Long)], NotUsed] = ???

  /**
   * @param offset Minimum value to retrieve
   * @param limit  Maximum number of values to retrieve
   * @return A Source of journal event sequence numbers (corresponding to the Ordering column)
   */
  override def journalSequence(offset: Long, limit: Long): Source[Long, NotUsed] = {
    val f = probe.ref.ask(JournalSequence(offset, limit)).mapTo[scala.collection.immutable.Seq[Long]]
    Source.future(f).mapConcat(identity)
  }

  /**
   * @return The value of the maximum (ordering) id in the journal
   */
  override def maxJournalSequence(): Future[Long] = Future.successful(0)

}
