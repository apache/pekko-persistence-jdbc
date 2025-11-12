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

package org.apache.pekko.persistence.jdbc.state.scaladsl

import com.typesafe.config.{ Config, ConfigFactory }

import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.apache.pekko
import pekko.actor._
import pekko.persistence.jdbc.db.SlickDatabase
import pekko.persistence.jdbc.config._
import pekko.persistence.jdbc.testkit.internal.{ H2, MySQL, Oracle, Postgres, SchemaType, SqlServer }
import pekko.persistence.jdbc.util.DropCreate
import pekko.serialization.SerializationExtension
import pekko.util.Timeout

import scala.concurrent.ExecutionContext

abstract class StateSpecBase(val config: Config, schemaType: SchemaType)
    extends AnyWordSpecLike
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with ScalaFutures
    with DropCreate
    with DataGenerationHelper {
  implicit def system: ActorSystem

  implicit lazy val e: ExecutionContext = system.dispatcher

  private[jdbc] def schemaTypeToProfile(s: SchemaType) = s match {
    case H2        => slick.jdbc.H2Profile
    case Postgres  => slick.jdbc.PostgresProfile
    case MySQL     => slick.jdbc.MySQLProfile
    case SqlServer => slick.jdbc.SQLServerProfile
    case Oracle    => slick.jdbc.OracleProfile
  }

  val customSerializers = ConfigFactory.parseString("""
      pekko.actor {
        serializers {
          my-payload = "org.apache.pekko.persistence.jdbc.state.MyPayloadSerializer"
        }
        serialization-bindings {
          "org.apache.pekko.persistence.jdbc.state.MyPayload" = my-payload
        }
      }
    """)

  val customConfig = ConfigFactory.parseString(s"""
    jdbc-durable-state-store {
      batchSize = 200
      refreshInterval = 300.milliseconds
      durable-state-sequence-retrieval {
        batch-size = 1000
        query-delay = 100.milliseconds
        max-tries = 3
      }
    }
  """)

  lazy val cfg = customConfig
    .getConfig("jdbc-durable-state-store")
    .withFallback(system.settings.config.getConfig("jdbc-durable-state-store"))
    .withFallback(config.getConfig("jdbc-durable-state-store"))
    .withFallback(customSerializers.getConfig("pekko.actor"))

  lazy val db = if (cfg.hasPath("slick.profile")) {
    SlickDatabase.database(cfg, new SlickConfiguration(cfg.getConfig("slick")), "slick.db")
  } else {
    // needed for integration test where we use postgres-shared-db-application.conf
    SlickDatabase.database(
      config,
      new SlickConfiguration(config.getConfig("pekko-persistence-jdbc.shared-databases.slick")),
      "pekko-persistence-jdbc.shared-databases.slick.db")
  }

  lazy val durableStateConfig = new DurableStateTableConfiguration(cfg)
  lazy val serialization = SerializationExtension(system)

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(60, Seconds), interval = Span(100, Millis))

  def withActorSystem(f: ExtendedActorSystem => Unit): Unit = {
    implicit val system: ExtendedActorSystem =
      ActorSystem("JdbcDurableStateSpec", config.withFallback(customSerializers)).asInstanceOf[ExtendedActorSystem]
    implicit val timeout: Timeout = Timeout(1.minute)
    try {
      f(system)
    } finally {
      system.actorSelection("system/" + "pekko-persistence-jdbc-durable-state-sequence-actor").resolveOne().onComplete {
        case Success(actorRef) =>
          system.stop(actorRef)
          Thread.sleep(1000)
          system.log.debug(s"Is terminated: ${actorRef.isTerminated}")
        case Failure(_) =>
          system.log.warning("system/" + "-persistence-jdbc-durable-state-sequence-actorsomename" + " does not exist")
      }
      system.terminate().futureValue
    }
  }

  override def beforeAll(): Unit = {
    dropAndCreate(schemaType)
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    dropAndCreate(schemaType)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    db.close()
    system.terminate().futureValue
  }
}
