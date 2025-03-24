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
import pekko.persistence.jdbc.config.SlickConfiguration
import pekko.persistence.jdbc.db.SlickDatabase
import pekko.persistence.jdbc.testkit.internal.Postgres
import pekko.persistence.jdbc.util.DropCreate
import pekko.persistence.state.DurableStateStoreRegistry
import pekko.util.Timeout
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }

import slick.jdbc.{ H2Profile, JdbcProfile }

import scala.concurrent.duration.DurationInt

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

abstract class DurableStateStorePostgresSchemaPluginSpec(val config: Config, profile: JdbcProfile)
    extends AnyWordSpecLike
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with ScalaFutures
    with DropCreate
    with DataGenerationHelper {

  val schemaName: String = "pekko"
  implicit val timeout: Timeout = Timeout(1.minute)
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(60, Seconds), interval = Span(100, Millis))

  val customConfig: Config = ConfigFactory.parseString(s"""
    jdbc-durable-state-store {
      tables {
        durable_state {
          schemaName = "pekko"
        }
      }
    }
  """)

  implicit lazy val system: ExtendedActorSystem =
    ActorSystem(
      "test",
      customConfig.withFallback(config)
    ).asInstanceOf[ExtendedActorSystem]

  lazy val db = SlickDatabase.database(
    config,
    new SlickConfiguration(config.getConfig("slick")), "slick.db"
  )

  private val createSchema = s"CREATE SCHEMA IF NOT EXISTS $schemaName;"
  private val moveDurableStateTableToSchema = s"alter table public.durable_state set schema $schemaName;"
  private val moveDurableStateTableToPublic = s"alter table $schemaName.durable_state set schema public;"
  private val createSchemaAndMoveTable = s"${createSchema}${moveDurableStateTableToSchema}"

  override def beforeAll(): Unit = {
    dropAndCreate(Postgres)
  }

  override def beforeEach(): Unit = {
    withStatement(_.execute(createSchemaAndMoveTable))
  }

  "A durable state store plugin" must {
    "instantiate a JdbcDurableDataStore successfully" in {

      val store = DurableStateStoreRegistry
        .get(system)
        .durableStateStoreFor[JdbcDurableStateStore[String]](JdbcDurableStateStore.Identifier)

      store shouldBe a[JdbcDurableStateStore[_]]
      store.system.settings.config shouldBe system.settings.config
      store.profile shouldBe profile
    }

    "persist states successfully" in {

      val store = DurableStateStoreRegistry
        .get(system)
        .durableStateStoreFor[JdbcDurableStateStore[String]](JdbcDurableStateStore.Identifier)

      upsertManyForOnePersistenceId(store, "durable_state", "durable-t1", 1, 400).size shouldBe 400

      eventually {
        store.maxStateStoreOffset().futureValue shouldBe 400
      }
    }
  }

  override def afterEach(): Unit = {
    withStatement(_.execute(moveDurableStateTableToPublic))
  }

  override def afterAll(): Unit = {
    system.terminate().futureValue

  }
}

class H2DurableStateStorePluginSpec
    extends DurableStateStorePluginSpec(ConfigFactory.load("h2-application.conf"), H2Profile)
