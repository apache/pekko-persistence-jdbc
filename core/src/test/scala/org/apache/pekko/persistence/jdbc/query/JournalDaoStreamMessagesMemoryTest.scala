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

package org.apache.pekko.persistence.jdbc.query

import java.lang.management.{ ManagementFactory, MemoryMXBean }
import java.util.UUID

import org.apache.pekko
import pekko.persistence.jdbc.query.JournalDaoStreamMessagesMemoryTest.fetchSize
import pekko.persistence.{ AtomicWrite, PersistentRepr }
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.stream.testkit.scaladsl.TestSink
import pekko.stream.{ Materializer, SystemMaterializer }
import com.typesafe.config.{ ConfigValue, ConfigValueFactory }
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object JournalDaoStreamMessagesMemoryTest {

  val fetchSize: Int = 100
  val MB: Int = 1024 * 1024

  val configOverrides: Map[String, ConfigValue] = Map(
    "jdbc-journal.fetch-size" -> ConfigValueFactory.fromAnyRef("100"))
}

abstract class JournalDaoStreamMessagesMemoryTest(configFile: String)
    extends QueryTestSpec(configFile, JournalDaoStreamMessagesMemoryTest.configOverrides) {

  import JournalDaoStreamMessagesMemoryTest.MB

  private val log = LoggerFactory.getLogger(this.getClass)

  val memoryMBean: MemoryMXBean = ManagementFactory.getMemoryMXBean

  it should "stream events" in withActorSystem { implicit system =>
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val mat: Materializer = SystemMaterializer(system).materializer

    withDao { dao =>
      val persistenceId = UUID.randomUUID().toString

      val writerUuid = UUID.randomUUID().toString

      val payloadSize = 5000 // 5000 bytes
      val eventsPerBatch = 1000

      val maxMem = 64 * MB

      val numberOfInsertBatches = {
        // calculate the number of batches using a factor to make sure we go a little bit over the limit
        (maxMem / (payloadSize * eventsPerBatch) * 1.2).round.toInt
      }
      val totalMessages = numberOfInsertBatches * eventsPerBatch
      val totalMessagePayload = totalMessages * payloadSize
      log.info(
        s"batches: $numberOfInsertBatches (with $eventsPerBatch events), total messages: $totalMessages, total msgs size: $totalMessagePayload")

      // payload can be the same when inserting to avoid unnecessary memory usage
      val payload = Array.fill(payloadSize)('a'.toByte)

      val lastInsert =
        Source
          .fromIterator(() => (1 to numberOfInsertBatches).iterator)
          .mapAsync(1) { i =>
            val end = i * eventsPerBatch
            val start = end - (eventsPerBatch - 1)
            log.info(s"batch $i - events from $start to $end")
            val atomicWrites =
              (start to end).map { j =>
                AtomicWrite(immutable.Seq(PersistentRepr(payload, j, persistenceId, writerUuid = writerUuid)))
              }
            dao.asyncWriteMessages(atomicWrites).map(_ => i)
          }
          .runWith(Sink.last)

      // wait until we write all messages
      // being very generous, 1 second per message
      lastInsert.futureValue(Timeout(totalMessages.seconds))

      log.info("Events written, starting replay")

      // sleep and gc to have some kind of stable measurement of current heap usage
      Thread.sleep(1000)
      System.gc()
      Thread.sleep(1000)
      val usedBefore = memoryMBean.getHeapMemoryUsage.getUsed

      val messagesSrc =
        dao.messagesWithBatch(persistenceId, 0, totalMessages, batchSize = fetchSize, None)
      val probe =
        messagesSrc
          .map {
            case Success((repr, _)) =>
              if (repr.sequenceNr % 100 == 0)
                log.info(s"fetched: ${repr.persistenceId} - ${repr.sequenceNr}/$totalMessages")
            case Failure(exception) =>
              log.error("Failure when reading messages.", exception)
          }
          .runWith(TestSink.probe)

      probe.request(10)
      probe.within(20.seconds) {
        probe.expectNextN(10)
      }

      // sleep and gc to have some kind of stable measurement of current heap usage
      Thread.sleep(2000)
      System.gc()
      Thread.sleep(1000)
      val usedAfter = memoryMBean.getHeapMemoryUsage.getUsed

      log.info(s"Used heap before ${usedBefore / MB} MB, after ${usedAfter / MB} MB")
      // actual usage is much less than 10 MB
      (usedAfter - usedBefore) should be <= (10L * MB)

      probe.cancel()
    }
  }
}

class H2JournalDaoStreamMessagesMemoryTest extends JournalDaoStreamMessagesMemoryTest("h2-application.conf")
    with H2Cleaner
