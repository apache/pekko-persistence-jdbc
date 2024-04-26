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

package org.apache.pekko.persistence.jdbc.journal.dao

import com.typesafe.config.{ ConfigValue, ConfigValueFactory }
import org.apache.pekko.persistence.jdbc.journal.dao.LimitWindowingStreamTest.fetchSize
import org.apache.pekko.persistence.jdbc.query.{ H2Cleaner, LegacyJournalDaoStreamMessagesMemoryTest, QueryTestSpec }
import org.apache.pekko.persistence.{ AtomicWrite, PersistentRepr }
import org.apache.pekko.serialization.SerializationExtension
import org.apache.pekko.stream.scaladsl.{ Keep, Sink, Source }
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
object LimitWindowingStreamTest {
  val fetchSize = 100
  val configOverrides: Map[String, ConfigValue] =
    Map("jdbc-journal.fetch-size" -> ConfigValueFactory.fromAnyRef(fetchSize))
}

abstract class LimitWindowingStreamTest(configFile: String)
    extends QueryTestSpec(configFile, LegacyJournalDaoStreamMessagesMemoryTest.configOverrides) {

  private val log = LoggerFactory.getLogger(this.getClass)

  it should "stream events with limit windowing" in withActorSystem { implicit system =>
    val persistenceId = UUID.randomUUID().toString
    val payload = 'a'.toByte
    val eventsPerBatch = 1000
    val numberOfInsertBatches = 16
    val totalMessages = numberOfInsertBatches * eventsPerBatch

    withDatabase { db =>
      implicit val ec: ExecutionContext = system.dispatcher

      val dao = new DefaultJournalDao(db, profile, journalConfig, SerializationExtension(system))

      val lastInsert =
        Source
          .fromIterator(() => (1 to numberOfInsertBatches).toIterator)
          .mapAsync(1) { i =>
            val end = i * eventsPerBatch
            val start = end - (eventsPerBatch - 1)
            log.info(s"batch $i (events from $start to $end")
            val atomicWrites =
              (start to end).map { j =>
                AtomicWrite(immutable.Seq(PersistentRepr(payload, j, persistenceId)))
              }
            dao.asyncWriteMessages(atomicWrites).map(_ => i)
          }
          .runWith(Sink.last)

      lastInsert.futureValue(Timeout(totalMessages.seconds))

      val messagesSrc = dao.internalBatchStream(persistenceId, 0, totalMessages, batchSize = fetchSize, None)

      val eventualSum: Future[(Int, Int)] = messagesSrc.toMat(Sink.fold((0, 0)) { case ((accBatch, accTotal), seq) =>
        (accBatch + 1, accTotal + seq.size)
      })(Keep.right).run()

      val (batchCount, totalCount) = Await.result(eventualSum, Duration.Inf)
      val totalBatch = totalMessages / fetchSize
      batchCount shouldBe totalBatch
      totalCount shouldBe totalMessages
    }
  }
}

class H2LimitWindowingStreamTest extends LimitWindowingStreamTest("h2-application.conf") with H2Cleaner
