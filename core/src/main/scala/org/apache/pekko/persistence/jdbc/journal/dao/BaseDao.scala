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
import pekko.persistence.jdbc.config.BaseDaoConfig
import pekko.stream.scaladsl.{ Keep, Sink, Source }
import pekko.stream.{ BoundedSourceQueue, Materializer, QueueOfferResult }

import scala.collection.immutable.{ Seq, Vector }
import scala.concurrent.{ ExecutionContext, Future, Promise }

// Shared with the legacy DAO
abstract class BaseDao[T] {
  implicit val mat: Materializer
  implicit val ec: ExecutionContext

  def baseDaoConfig: BaseDaoConfig

  private val writeQueue: BoundedSourceQueue[(Promise[Unit], Seq[T])] = Source
    .queue[(Promise[Unit], Seq[T])](baseDaoConfig.bufferSize)
    .batchWeighted[(Seq[Promise[Unit]], Seq[T])](baseDaoConfig.batchSize, _._2.size, tup => Vector(tup._1) -> tup._2) {
      case ((promises, rows), (newPromise, newRows)) => (promises :+ newPromise) -> (rows ++ newRows)
    }
    .mapAsync(baseDaoConfig.parallelism) { case (promises, rows) =>
      writeJournalRows(rows).map(unit => promises.foreach(_.success(unit))).recover { case t =>
        promises.foreach(_.failure(t))
      }
    }
    .toMat(Sink.ignore)(Keep.left)
    .run()

  def writeJournalRows(xs: Seq[T]): Future[Unit]

  def queueWriteJournalRows(xs: Seq[T]): Future[Unit] = {
    val promise = Promise[Unit]()
    writeQueue.offer(promise -> xs) match {
      case QueueOfferResult.Enqueued =>
        promise.future
      case QueueOfferResult.Failure(t) =>
        Future.failed(new Exception("Failed to write journal row batch", t))
      case QueueOfferResult.Dropped =>
        Future.failed(new Exception(
          s"Failed to enqueue journal row batch write, the queue buffer was full (${baseDaoConfig.bufferSize} elements) please check the jdbc-journal.bufferSize setting"))
      case QueueOfferResult.QueueClosed =>
        Future.failed(new Exception("Failed to enqueue journal row batch write, the queue was closed"))
    }
  }

}
