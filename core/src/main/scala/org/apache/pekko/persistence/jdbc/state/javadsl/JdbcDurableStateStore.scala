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

package org.apache.pekko.persistence.jdbc.state.javadsl

import java.util.Optional
import java.util.concurrent.CompletionStage
import scala.concurrent.ExecutionContext
import org.apache.pekko
import pekko.annotation.ApiMayChange
import pekko.{ Done, NotUsed }
import pekko.persistence.jdbc.state.DurableStateQueries
import pekko.persistence.jdbc.config.DurableStateTableConfiguration
import pekko.persistence.jdbc.state.scaladsl.{ JdbcDurableStateStore => ScalaJdbcDurableStateStore }
import pekko.persistence.query.{ DurableStateChange, Offset }
import pekko.persistence.query.javadsl.DurableStateStoreQuery
import pekko.persistence.state.javadsl.{ DurableStateUpdateStore, GetObjectResult }
import pekko.stream.javadsl.Source
import pekko.util.FutureConverters._
import slick.jdbc.JdbcProfile

object JdbcDurableStateStore {
  val Identifier = ScalaJdbcDurableStateStore.Identifier
}

/**
 * API may change
 */
@ApiMayChange
class JdbcDurableStateStore[A](
    profile: JdbcProfile,
    durableStateConfig: DurableStateTableConfiguration,
    scalaStore: ScalaJdbcDurableStateStore[A])(implicit ec: ExecutionContext)
    extends DurableStateUpdateStore[A]
    with DurableStateStoreQuery[A] {

  val queries = new DurableStateQueries(profile, durableStateConfig)

  def getObject(persistenceId: String): CompletionStage[GetObjectResult[A]] =
    scalaStore
      .getObject(persistenceId)
      .map(x => GetObjectResult(Optional.ofNullable(x.value.getOrElse(null.asInstanceOf[A])), x.revision)).asJava

  def upsertObject(persistenceId: String, revision: Long, value: A, tag: String): CompletionStage[Done] =
    scalaStore.upsertObject(persistenceId, revision, value, tag).asJava

  def deleteObject(persistenceId: String): CompletionStage[Done] =
    scalaStore.deleteObject(persistenceId).asJava

  def deleteObject(persistenceId: String, revision: Long): CompletionStage[Done] =
    scalaStore.deleteObject(persistenceId, revision).asJava

  def currentChanges(tag: String, offset: Offset): Source[DurableStateChange[A], NotUsed] =
    scalaStore.currentChanges(tag, offset).asJava

  def changes(tag: String, offset: Offset): Source[DurableStateChange[A], NotUsed] =
    scalaStore.changes(tag, offset).asJava
}
