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
import pekko.actor.Scheduler
import pekko.annotation.InternalApi
import pekko.persistence.PersistentRepr
import pekko.stream.Materializer
import pekko.stream.scaladsl.{ Sink, Source }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

trait BaseJournalDaoWithReadMessages extends JournalDaoWithReadMessages {
  import BaseJournalDaoWithReadMessages._

  implicit val ec: ExecutionContext
  implicit val mat: Materializer

  override def messagesWithBatch(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      batchSize: Int,
      refreshInterval: Option[(FiniteDuration, Scheduler)]): Source[Try[(PersistentRepr, Long)], NotUsed] = {
    internalBatchStream(persistenceId, fromSequenceNr, toSequenceNr, batchSize, refreshInterval).mapConcat(identity)
  }

  /**
   * Separate this method for unit tests.
   */
  @InternalApi
  private[dao] def internalBatchStream(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      batchSize: Int,
      refreshInterval: Option[(FiniteDuration, Scheduler)]
  ): Source[Seq[Try[(PersistentRepr, Long)]], NotUsed] = {

    val normalizedBatchSize = Math.max(1, batchSize)
    val normalizedFromSequenceNr = Math.max(1, fromSequenceNr)

    def pollOrComplete(from: Long): QueryPlan = refreshInterval match {
      case Some((delay, scheduler)) => PollRemaining(from, delay, scheduler)
      case None                     => Complete
    }

    // A windowed query spans [from, min(from + batchSize, toSequenceNr)]; at full width that is
    // batchSize + 1 sequence numbers, which lets a window tolerate one missing sequence number
    // and still return a full batch.
    def windowedQuery(from: Long): QueryWindow = {
      val endInclusive =
        if (from <= (Long.MaxValue - normalizedBatchSize)) Math.min(from + normalizedBatchSize, toSequenceNr)
        else toSequenceNr // from + batchSize overflows; fall back to the full remaining range
      QueryWindow(from, endInclusive)
    }

    def retrieveBatch(from: Long, endInclusive: Long): Future[Option[(QueryPlan, Seq[Try[(PersistentRepr, Long)]])]] =
      messages(persistenceId, from, endInclusive, normalizedBatchSize).runWith(Sink.seq).map { batch =>
        // Messages are ordered by sequence number, therefore the last one is the largest
        val lastSeqNrInBatch: Option[Long] = batch.lastOption match {
          case Some(Success((repr, _))) => Some(repr.sequenceNr)
          case Some(Failure(cause))     => throw cause // fail the returned Future
          case None                     => None
        }
        val hasReachedToSequenceNr = lastSeqNrInBatch.exists(_ >= toSequenceNr)
        val isFullBatch = batch.size == normalizedBatchSize
        val wasWindowedQuery = endInclusive < toSequenceNr
        // fromSequenceNr beyond toSequenceNr requests an empty range, which must complete
        // rather than poll a range that can never produce messages
        val isEmptyRange = from > toSequenceNr
        val nextFrom: Long = lastSeqNrInBatch.map(_ + 1).getOrElse(from)

        // Deleted messages leave gaps, so a window may hold fewer live messages than batchSize
        // even when more messages exist beyond it. A short batch is conclusive only when the
        // queried range reached toSequenceNr, otherwise requery the full remaining range.
        val nextQueryPlan: QueryPlan =
          if (hasReachedToSequenceNr || isEmptyRange) Complete
          else if (isFullBatch) windowedQuery(nextFrom)
          else if (wasWindowedQuery) QueryRemaining(nextFrom)
          else pollOrComplete(nextFrom)

        Some((nextQueryPlan, batch))
      }

    // A full-range first query crosses a leading gap (e.g. journal purged up to a snapshot)
    // in a single round trip; the LIMITed query costs the same as a windowed one when dense.
    // Once a full batch shows the journal to be dense, reads switch to bounded windows, the
    // perf safeguard introduced by PR #180, and fall back to a full-range query on a gap.
    Source
      .unfoldAsync[QueryPlan, Seq[Try[(PersistentRepr, Long)]]](QueryRemaining(normalizedFromSequenceNr)) {
        case Complete =>
          Future.successful(None)
        case QueryWindow(from, endInclusive) =>
          retrieveBatch(from, endInclusive)
        case QueryRemaining(from) =>
          retrieveBatch(from, toSequenceNr)
        case PollRemaining(from, delay, scheduler) =>
          pekko.pattern.after(delay, scheduler)(retrieveBatch(from, toSequenceNr))
      }
  }
}

private[dao] object BaseJournalDaoWithReadMessages {

  /** The query the batch stream will run. */
  private sealed trait QueryPlan

  /**
   * Query the window [from, endInclusive]: the dense fast path, planned only after a full batch
   * showed the messages to be dense. Windowing bounds the range a query scans; it is
   * not load-bearing for correctness, since a short window falls back to [[QueryRemaining]].
   */
  private final case class QueryWindow(from: Long, endInclusive: Long) extends QueryPlan

  /**
   * Query the whole remaining range [from, toSequenceNr]: planned first, and whenever a short
   * windowed batch leaves the remainder undetermined, since gaps cannot hide messages from an
   * unwindowed query.
   */
  private final case class QueryRemaining(from: Long) extends QueryPlan

  /** Poll the remaining range after `delay`: the live tail, when the range is exhausted but may grow. */
  private final case class PollRemaining(from: Long, delay: FiniteDuration, scheduler: Scheduler) extends QueryPlan

  private case object Complete extends QueryPlan
}
