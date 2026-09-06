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

package org.apache.pekko.persistence.jdbc.state

import scala.concurrent.duration._

import org.apache.pekko
import pekko.actor.{ ActorSystem, ExtendedActorSystem }
import pekko.persistence.jdbc.util.ConnectionPools
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class JdbcDurableStateStoreProviderSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 1.minute)

  "A JdbcDurableStateStoreProvider" must {
    "close the database it created when the actor system terminates" in {
      val system =
        ActorSystem("JdbcDurableStateStoreProviderSpec", ConfigFactory.load("h2-application.conf"))
          .asInstanceOf[ExtendedActorSystem]
      val provider = new JdbcDurableStateStoreProvider[String](system)

      provider.slickDb.allowShutdown shouldBe true
      ConnectionPools.isClosed(provider.db) shouldBe false

      system.terminate().futureValue

      ConnectionPools.isClosed(provider.db) shouldBe true
    }
  }
}
