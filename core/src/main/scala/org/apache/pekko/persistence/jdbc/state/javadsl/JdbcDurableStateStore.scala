/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.state.javadsl

import java.util.Optional
import java.util.concurrent.CompletionStage

import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext

import org.apache.pekko.annotation.ApiMayChange
import org.apache.pekko.{ Done, NotUsed }
import org.apache.pekko.persistence.state.javadsl.{ DurableStateUpdateStore, GetObjectResult }
import org.apache.pekko.persistence.jdbc.state.DurableStateQueries
import org.apache.pekko.persistence.jdbc.config.DurableStateTableConfiguration
import org.apache.pekko.persistence.jdbc.state.scaladsl.{ JdbcDurableStateStore => ScalaJdbcDurableStateStore }
import org.apache.pekko.persistence.query.{ DurableStateChange, Offset }
import org.apache.pekko.persistence.query.javadsl.DurableStateStoreQuery
import org.apache.pekko.stream.javadsl.Source
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
    scalaStore: org.apache.pekko.persistence.jdbc.state.scaladsl.JdbcDurableStateStore[A])(implicit ec: ExecutionContext)
    extends DurableStateUpdateStore[A]
    with DurableStateStoreQuery[A] {

  val queries = new DurableStateQueries(profile, durableStateConfig)

  def getObject(persistenceId: String): CompletionStage[GetObjectResult[A]] =
    toJava(
      scalaStore
        .getObject(persistenceId)
        .map(x => GetObjectResult(Optional.ofNullable(x.value.getOrElse(null.asInstanceOf[A])), x.revision)))

  def upsertObject(persistenceId: String, revision: Long, value: A, tag: String): CompletionStage[Done] =
    toJava(scalaStore.upsertObject(persistenceId, revision, value, tag))

  def deleteObject(persistenceId: String): CompletionStage[Done] =
    toJava(scalaStore.deleteObject(persistenceId))

  def currentChanges(tag: String, offset: Offset): Source[DurableStateChange[A], NotUsed] =
    scalaStore.currentChanges(tag, offset).asJava

  def changes(tag: String, offset: Offset): Source[DurableStateChange[A], NotUsed] =
    scalaStore.changes(tag, offset).asJava
}
