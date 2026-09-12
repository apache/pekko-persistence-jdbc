/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.persistence.jdbc.journal.dao

import java.util.concurrent.ConcurrentLinkedQueue

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.persistence.jdbc.config.BaseDaoConfig
import pekko.stream.{ Materializer, SystemMaterializer }
import pekko.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

object BaseDaoSpec {
  class TestDao(override val baseDaoConfig: BaseDaoConfig)(
      implicit val ec: ExecutionContext,
      val mat: Materializer)
      extends BaseDao[String] {

    val written = new ConcurrentLinkedQueue[String]

    override def writeJournalRows(xs: Seq[String]): Future[Unit] = {
      xs.foreach(written.add)
      Future.unit
    }
  }
}

class BaseDaoSpec extends TestKit(ActorSystem("BaseDaoSpec")) with AnyWordSpecLike with Matchers with ScalaFutures
    with BeforeAndAfterAll {
  import BaseDaoSpec._

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 1.minute)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = SystemMaterializer(system).materializer

  private val daoConfig = new BaseDaoConfig(
    ConfigFactory.parseString("""
      bufferSize = 100
      batchSize = 10
      replayBatchSize = 10
      parallelism = 1
      """))

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "BaseDao" must {
    "write the rows that are queued" in {
      val dao = new TestDao(daoConfig)

      dao.queueWriteJournalRows(Seq("a", "b")).futureValue

      dao.written.asScala.toList shouldBe List("a", "b")
    }

    "reject rows once the write queue has been completed" in {
      val dao = new TestDao(daoConfig)
      dao.queueWriteJournalRows(Seq("a")).futureValue

      dao.completeWriteQueue()

      val failure = dao.queueWriteJournalRows(Seq("b")).failed.futureValue
      failure.getMessage should include("the queue was closed")
      dao.written.asScala.toList shouldBe List("a")
    }
  }
}
