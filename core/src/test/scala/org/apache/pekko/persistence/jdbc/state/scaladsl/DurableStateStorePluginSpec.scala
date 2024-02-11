/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.state.scaladsl

import com.typesafe.config.{ Config, ConfigFactory }
import org.apache.pekko
import pekko.actor._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import pekko.persistence.state.DurableStateStoreRegistry
import slick.jdbc.{ H2Profile, JdbcProfile }

abstract class DurableStateStorePluginSpec(config: Config, profile: JdbcProfile)
    extends AnyWordSpecLike
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {

  implicit lazy val system: ExtendedActorSystem =
    ActorSystem("test", config).asInstanceOf[ExtendedActorSystem]

  "A durable state store plugin" must {
    "instantiate a JdbcDurableDataStore successfully" in {
      val store = DurableStateStoreRegistry
        .get(system)
        .durableStateStoreFor[JdbcDurableStateStore[String]](JdbcDurableStateStore.Identifier)

      store shouldBe a[JdbcDurableStateStore[_]]
      store.system.settings.config shouldBe system.settings.config
      store.profile shouldBe profile
    }
  }

  override def afterAll(): Unit = {
    system.terminate().futureValue
  }
}

class H2DurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("h2-application.conf"), H2Profile)
