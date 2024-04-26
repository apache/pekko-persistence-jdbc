/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.pekko.persistence.jdbc.query

import com.typesafe.config.{ ConfigValue, ConfigValueFactory }
import org.apache.pekko.persistence.jdbc.journal.dao.DefaultJournalDao
import org.apache.pekko.persistence.jdbc.query.DefaultJournalDaoStreamMessagesMemoryTest.MB
import org.apache.pekko.persistence.{ AtomicWrite, PersistentRepr }
import org.apache.pekko.serialization.SerializationExtension
import org.apache.pekko.stream.scaladsl.{ Sink, Source }
import org.apache.pekko.stream.testkit.scaladsl.TestSink
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.slf4j.LoggerFactory

import java.lang.management.{ ManagementFactory, MemoryMXBean }
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success }

object DefaultJournalDaoStreamMessagesMemoryTest {

  val fetchSize: Int = 100
  val MB: Int = 1024 * 1024

  val configOverrides: Map[String, ConfigValue] = Map(
    "jdbc-journal.fetch-size" -> ConfigValueFactory.fromAnyRef(fetchSize))

}

abstract class DefaultJournalDaoStreamMessagesMemoryTest(configFile: String)
    extends QueryTestSpec(configFile, DefaultJournalDaoStreamMessagesMemoryTest.configOverrides) {
  private val log = LoggerFactory.getLogger(this.getClass)

  val memoryMBean: MemoryMXBean = ManagementFactory.getMemoryMXBean

  it should "stream events" in withActorSystem { implicit system =>
    withDatabase { db =>
      implicit val ec: ExecutionContext = system.dispatcher

      val persistenceId = UUID.randomUUID().toString
      val dao = new DefaultJournalDao(db, profile, journalConfig, SerializationExtension(system))

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
          .fromIterator(() => (1 to numberOfInsertBatches).toIterator)
          .mapAsync(1) { i =>
            val end = i * eventsPerBatch
            val start = end - (eventsPerBatch - 1)
            log.info(s"batch $i - events from $start to $end")
            val atomicWrites =
              (start to end).map { j =>
                AtomicWrite(immutable.Seq(PersistentRepr(payload, j, persistenceId)))
              }.toSeq

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
        dao.messagesWithBatch(persistenceId, 0, totalMessages, batchSize = 100, None)
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

class H2DefaultJournalDaoStreamMessagesMemoryTest
    extends DefaultJournalDaoStreamMessagesMemoryTest("h2-application.conf")
    with H2Cleaner
