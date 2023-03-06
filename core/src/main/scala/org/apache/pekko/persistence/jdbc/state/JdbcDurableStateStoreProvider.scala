/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.state

import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcProfile
import slick.jdbc.JdbcBackend._
import org.apache.pekko.actor.ExtendedActorSystem
import org.apache.pekko.persistence.jdbc.config.DurableStateTableConfiguration
import org.apache.pekko.persistence.state.scaladsl.DurableStateStore
import org.apache.pekko.persistence.state.javadsl.{ DurableStateStore => JDurableStateStore }
import org.apache.pekko.persistence.state.DurableStateStoreProvider
import org.apache.pekko.persistence.jdbc.db.{ SlickDatabase, SlickExtension }
import org.apache.pekko.serialization.SerializationExtension
import org.apache.pekko.stream.{ Materializer, SystemMaterializer }

class JdbcDurableStateStoreProvider[A](system: ExtendedActorSystem) extends DurableStateStoreProvider {

  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = SystemMaterializer(system).materializer

  val config = system.settings.config

  val slickDb: SlickDatabase =
    SlickExtension(system).database(config.getConfig(scaladsl.JdbcDurableStateStore.Identifier))
  def db: Database = slickDb.database

  lazy val durableStateConfig = new DurableStateTableConfiguration(
    config.getConfig(scaladsl.JdbcDurableStateStore.Identifier))
  lazy val serialization = SerializationExtension(system)
  val profile: JdbcProfile = slickDb.profile

  override val scaladslDurableStateStore: DurableStateStore[Any] =
    new scaladsl.JdbcDurableStateStore[Any](db, profile, durableStateConfig, serialization)(system)

  override val javadslDurableStateStore: JDurableStateStore[AnyRef] =
    new javadsl.JdbcDurableStateStore[AnyRef](
      profile,
      durableStateConfig,
      new scaladsl.JdbcDurableStateStore[AnyRef](db, profile, durableStateConfig, serialization)(system))
}
