/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.journal.dao

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ActorSystem
import pekko.persistence.PersistentRepr
import pekko.persistence.jdbc.SimpleSpec
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.stream.testkit.TestSubscriber
import pekko.stream.testkit.scaladsl.TestSink
import pekko.stream.{ Materializer, SystemMaterializer }

import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

/**
 * Specifies the batched message stream against an in-memory journal, where a missing
 * sequence number stands for a deleted message. The stream must emit all live messages
 * regardless of such gaps. Database-coupled behavior (soft-delete filtering, serialization
 * failures, per-database query semantics) is covered by MessagesWithBatchDatabaseContractTest.
 */
final class MessagesWithBatchTest extends SimpleSpec {

  private implicit val system: ActorSystem = ActorSystem("MessagesWithBatchTest")
  private implicit val materializer: Materializer = SystemMaterializer(system).materializer

  private val persistenceId = "pid"

  // SimpleSpec leaves ScalaFutures' default 150ms patience in place, too tight for CI
  private implicit val pc: PatienceConfig = PatienceConfig(timeout = 10.seconds)

  override def afterAll(): Unit = {
    system.terminate().futureValue
    super.afterAll()
  }

  "messagesWithBatch" should "emit all messages when a single missing message lies within the batch window" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 3L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 3, batchSize = 2)

    emitted shouldBe Seq(1L, 3L)
  }

  it should "emit all messages when a single missing message lies within the batch window of size one" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 3L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 3, batchSize = 1)

    emitted shouldBe Seq(1L, 3L)
  }

  it should "emit all messages when a gap is wider than the batch size" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 4L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 4, batchSize = 1)

    emitted shouldBe Seq(1L, 4L)
  }

  it should "emit all messages when a batch window contains fewer live messages than the batch size" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 2L, 4L, 6L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 6, batchSize = 2)

    emitted shouldBe Seq(1L, 2L, 4L, 6L)
  }

  it should "emit messages beyond a gap wider than the batch size when polling with a refresh interval" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 4L))

    val emitted =
      emittedSequenceNrs(dao, toSequenceNr = 4, batchSize = 1, count = Some(2), refreshInterval = Some(50.millis))

    emitted shouldBe Seq(1L, 4L)
  }

  it should "emit all messages when messages at the start of the journal are missing" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(3L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 3, batchSize = 1)

    emitted shouldBe Seq(3L)
  }

  it should "emit all messages when a gap is wider than the batch size up to an unbounded toSequenceNr" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 4L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = Long.MaxValue, batchSize = 1)

    emitted shouldBe Seq(1L, 4L)
  }

  it should "emit messages beyond a gap when polling up to an unbounded toSequenceNr" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 4L))

    val emitted =
      emittedSequenceNrs(dao, toSequenceNr = Long.MaxValue, batchSize = 1, count = Some(2),
        refreshInterval = Some(50.millis))

    emitted shouldBe Seq(1L, 4L)
  }

  it should "emit all messages when the batch size is zero" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 2L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 2, batchSize = 0)

    emitted shouldBe Seq(1L, 2L)
  }

  it should "emit all messages across multiple gaps wider than the batch size" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 4L, 5L, 8L, 9L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 9, batchSize = 1)

    emitted shouldBe Seq(1L, 4L, 5L, 8L, 9L)
  }

  it should "keep emitting newly appended messages after crossing a gap in a live stream" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 4L))
    val probe = sequenceNrProbe(dao, toSequenceNr = Long.MaxValue, batchSize = 1, refreshInterval = Some(50.millis))

    probe.request(4)
    probe.expectNext(1L, 4L)
    dao.append(5L, 6L)

    probe.expectNext(5L, 6L)
    probe.cancel()
  }

  it should "emit only newly appended messages when polling resumes after a short non-empty batch" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L))
    val probe = sequenceNrProbe(dao, toSequenceNr = Long.MaxValue, batchSize = 2, refreshInterval = Some(50.millis))

    probe.request(3)
    probe.expectNext(1L)
    dao.append(2L, 3L)

    probe.expectNext(2L, 3L)
    probe.cancel()
  }

  it should "complete after emitting all messages when the journal ends with a gap" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 2L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 4, batchSize = 2)

    emitted shouldBe Seq(1L, 2L)
  }

  it should
  "keep polling without emitting or completing when a live stream reaches a trailing gap below toSequenceNr" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 2L))
    val probe = sequenceNrProbe(dao, toSequenceNr = 4, batchSize = 2, refreshInterval = Some(50.millis))

    probe.request(4)
    probe.expectNext(1L, 2L)

    probe.expectNoMessage(500.millis)
    probe.cancel()
  }

  it should "complete a bounded live stream once appended messages reach toSequenceNr" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 2L))
    val probe = sequenceNrProbe(dao, toSequenceNr = 4, batchSize = 2, refreshInterval = Some(50.millis))

    probe.request(5)
    probe.expectNext(1L, 2L)
    dao.append(3L, 4L)

    probe.expectNext(3L, 4L)
    probe.expectComplete()
  }

  it should "emit messages with sequence numbers near Long.MaxValue without overflowing the batch window" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(Long.MaxValue - 2, Long.MaxValue - 1))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = Long.MaxValue, batchSize = 1,
      fromSequenceNr = Long.MaxValue - 2)

    emitted shouldBe Seq(Long.MaxValue - 2, Long.MaxValue - 1)
  }

  it should "complete with no messages when the journal is empty" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq.empty)

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 3, batchSize = 1)

    emitted shouldBe empty
  }

  it should "emit messages from a fromSequenceNr that lies inside a gap" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 4L, 5L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 5, batchSize = 1, fromSequenceNr = 2)

    emitted shouldBe Seq(4L, 5L)
  }

  // The read journal defaults fromSequenceNr to 0, below the journal's 1-based numbering
  it should "emit all messages when fromSequenceNr is zero" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 2L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 2, batchSize = 1, fromSequenceNr = 0)

    emitted shouldBe Seq(1L, 2L)
  }

  it should "complete without emitting when fromSequenceNr exceeds toSequenceNr" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 2L, 3L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 2, batchSize = 1, fromSequenceNr = 3)

    emitted shouldBe empty
  }

  it should "not emit messages beyond toSequenceNr when crossing a gap" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 5L, 6L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 4, batchSize = 1)

    emitted shouldBe Seq(1L)
  }

  it should "emit all messages when single-message gaps occur in separate batch windows" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 3L, 4L, 6L, 7L, 9L))

    val emitted = emittedSequenceNrs(dao, toSequenceNr = 9, batchSize = 1)

    emitted shouldBe Seq(1L, 3L, 4L, 6L, 7L, 9L)
  }

  it should "fail the stream when the last message of a batch cannot be read" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 2L), corruptSequenceNrs = Set(2L))

    val result = dao
      .messagesWithBatch(persistenceId, fromSequenceNr = 1, toSequenceNr = 2, batchSize = 2, refreshInterval = None)
      .runWith(Sink.seq)

    result.failed.futureValue shouldBe a[CorruptMessageException]
  }

  // Only a Failure in the last position fails the stream (the next query needs its sequence
  // number); a mid-batch Failure is passed through for the consumer to handle
  it should "emit an unreadable message as a failed element followed by the remaining messages" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 2L, 3L), corruptSequenceNrs = Set(2L))

    val emitted = dao
      .messagesWithBatch(persistenceId, fromSequenceNr = 1, toSequenceNr = 3, batchSize = 3, refreshInterval = None)
      .runWith(Sink.seq)
      .futureValue

    emitted.map(_.toOption.map { case (repr, _) => repr.sequenceNr }) shouldBe Seq(Some(1L), None, Some(3L))
  }

  // The first batch query must span the entire remaining range, so a journal whose head is
  // deleted (e.g. purged up to a snapshot) is crossed in a single round trip instead of an
  // empty windowed query followed by a full-range one.
  it should "cross a leading gap with a single query spanning the full remaining range" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(100L, 101L, 102L))

    dao
      .messagesWithBatch(persistenceId, fromSequenceNr = 1, toSequenceNr = 102, batchSize = 3, refreshInterval = None)
      .runWith(Sink.seq)
      .futureValue

    dao.queriedRanges shouldBe Seq(1L -> 102L)
  }

  // Once a full batch shows the journal to be dense, subsequent queries must be bounded
  // windows rather than full-range scans: the perf safeguard introduced by PR #180
  it should "query bounded windows after a full batch shows the journal to be dense" in {
    val dao = new InMemoryJournalDao(liveSequenceNrs = Seq(1L, 2L, 3L, 4L, 5L, 6L))

    dao
      .messagesWithBatch(persistenceId, fromSequenceNr = 1, toSequenceNr = 6, batchSize = 2, refreshInterval = None)
      .runWith(Sink.seq)
      .futureValue

    dao.queriedRanges shouldBe Seq(1L -> 6L, 3L -> 5L, 5L -> 6L)
  }

  /**
   * Collects the sequence numbers the batched message stream emits. A polling stream never
   * completes on its own, so pass `count` to bound it.
   */
  private def emittedSequenceNrs(dao: InMemoryJournalDao, toSequenceNr: Long, batchSize: Int,
      fromSequenceNr: Long =
        1, refreshInterval: Option[FiniteDuration] = None, count: Option[Int] = None): Seq[Long] = {
    val sequenceNrs = sequenceNrSource(dao, fromSequenceNr, toSequenceNr, batchSize, refreshInterval)
    count
      .fold(sequenceNrs)(sequenceNrs.take(_))
      .completionTimeout(10.seconds)
      .runWith(Sink.seq)
      .futureValue
  }

  private def sequenceNrProbe(dao: InMemoryJournalDao, toSequenceNr: Long, batchSize: Int,
      refreshInterval: Option[FiniteDuration]): TestSubscriber.Probe[Long] =
    sequenceNrSource(dao, fromSequenceNr = 1, toSequenceNr, batchSize, refreshInterval)
      .runWith(TestSink[Long]())

  private def sequenceNrSource(dao: InMemoryJournalDao, fromSequenceNr: Long, toSequenceNr: Long,
      batchSize: Int, refreshInterval: Option[FiniteDuration]): Source[Long, NotUsed] =
    dao
      .messagesWithBatch(persistenceId, fromSequenceNr, toSequenceNr, batchSize,
        refreshInterval.map(_ -> system.scheduler))
      .mapAsync(1)(Future.fromTry)
      .map { case (repr, _) => repr.sequenceNr }

  /**
   * Serves batches from an in-memory set of live sequence numbers, honoring the contract of
   * `messages`: ascending order, inclusive bounds, at most `max` elements. Sequence numbers
   * in `corruptSequenceNrs` yield Failure elements, as unreadable rows do. Records each
   * queried range and supports appends for live-stream tests.
   */
  private final class InMemoryJournalDao(liveSequenceNrs: Seq[Long], corruptSequenceNrs: Set[Long] = Set.empty)
      extends BaseJournalDaoWithReadMessages {
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val mat: Materializer = materializer

    @volatile private var sequenceNrs: Vector[Long] = liveSequenceNrs.sorted.toVector
    private val ranges = new ConcurrentLinkedQueue[(Long, Long)]

    def queriedRanges: Seq[(Long, Long)] = ranges.asScala.toSeq

    def append(appendedSequenceNrs: Long*): Unit =
      sequenceNrs = (sequenceNrs ++ appendedSequenceNrs).sorted

    override def messages(
        persistenceId: String,
        fromSequenceNr: Long,
        toSequenceNr: Long,
        max: Long): Source[Try[(PersistentRepr, Long)], NotUsed] = {
      ranges.add(fromSequenceNr -> toSequenceNr)
      val batch = sequenceNrs
        .filter(sequenceNr => sequenceNr >= fromSequenceNr && sequenceNr <= toSequenceNr)
        .take(max.toInt)
        .map { sequenceNr =>
          if (corruptSequenceNrs.contains(sequenceNr)) Failure(new CorruptMessageException(sequenceNr))
          else Success(PersistentRepr("payload", sequenceNr, persistenceId) -> sequenceNr)
        }
      Source(batch.toList)
    }
  }

  private final class CorruptMessageException(sequenceNr: Long)
      extends RuntimeException(s"Cannot read message $sequenceNr")
}
