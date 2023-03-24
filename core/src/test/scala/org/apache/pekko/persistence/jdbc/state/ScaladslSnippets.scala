/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.state

import scala.concurrent.Future
import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.actor.ActorSystem
import pekko.Done
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers

object ScaladslSnippets extends ScalaFutures with Matchers {

  def create(): Unit = {
    // #create
    import org.apache.pekko.persistence.jdbc.testkit.scaladsl.SchemaUtils

    implicit val system: ActorSystem = ActorSystem("example")
    val _: Future[Done] = SchemaUtils.createIfNotExists()
    // #create
  }

  def durableStatePlugin(): Unit = {
    implicit val system: ActorSystem = ActorSystem()

    // #jdbc-durable-state-store
    import org.apache.pekko
    import pekko.persistence.state.DurableStateStoreRegistry
    import pekko.persistence.jdbc.state.scaladsl.JdbcDurableStateStore
    val store = DurableStateStoreRegistry
      .get(system)
      .durableStateStoreFor[JdbcDurableStateStore[String]](JdbcDurableStateStore.Identifier)
    // #jdbc-durable-state-store
  }

  def getObject(): Unit = {
    implicit val system: ActorSystem = ActorSystem()

    // #get-object
    import org.apache.pekko
    import pekko.persistence.state.DurableStateStoreRegistry
    import pekko.persistence.jdbc.state.scaladsl.JdbcDurableStateStore
    import pekko.persistence.state.scaladsl.GetObjectResult

    val store = DurableStateStoreRegistry
      .get(system)
      .durableStateStoreFor[JdbcDurableStateStore[String]](JdbcDurableStateStore.Identifier)

    val futureResult: Future[GetObjectResult[String]] = store.getObject("InvalidPersistenceId")
    futureResult.futureValue.value shouldBe None
    // #get-object
  }

  def upsertAndGetObject(): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val e = system.dispatcher

    // #upsert-get-object
    import org.apache.pekko
    import pekko.persistence.state.DurableStateStoreRegistry
    import pekko.persistence.jdbc.state.scaladsl.JdbcDurableStateStore
    import pekko.persistence.state.scaladsl.GetObjectResult

    val store = DurableStateStoreRegistry
      .get(system)
      .durableStateStoreFor[JdbcDurableStateStore[String]](JdbcDurableStateStore.Identifier)

    val v: Future[GetObjectResult[String]] =
      for {
        n <- store.upsertObject("p234", 1, "a valid string", "t123")
        _ = n shouldBe pekko.Done
        g <- store.getObject("p234")
        _ = g.value shouldBe Some("a valid string")
        u <- store.upsertObject("p234", 2, "updated valid string", "t123")
        _ = u shouldBe pekko.Done
        h <- store.getObject("p234")
      } yield h

    v.futureValue.value shouldBe Some("updated valid string")
    // #upsert-get-object
  }

  def deleteObject(): Unit = {
    implicit val system: ActorSystem = ActorSystem()

    // #delete-object
    import org.apache.pekko
    import pekko.persistence.state.DurableStateStoreRegistry
    import pekko.persistence.jdbc.state.scaladsl.JdbcDurableStateStore

    val store = DurableStateStoreRegistry
      .get(system)
      .durableStateStoreFor[JdbcDurableStateStore[String]](JdbcDurableStateStore.Identifier)

    store.deleteObject("p123").futureValue shouldBe Done
    store.getObject("p123").futureValue.value shouldBe None
    // #delete-object
  }

  def currentChanges(): Unit = {
    implicit val system: ActorSystem = ActorSystem()

    // #current-changes
    import org.apache.pekko
    import pekko.NotUsed
    import pekko.stream.scaladsl.Source
    import pekko.persistence.state.DurableStateStoreRegistry
    import pekko.persistence.jdbc.state.scaladsl.JdbcDurableStateStore
    import pekko.persistence.query.{ DurableStateChange, NoOffset }

    val store = DurableStateStoreRegistry
      .get(system)
      .durableStateStoreFor[JdbcDurableStateStore[String]](JdbcDurableStateStore.Identifier)

    val willCompleteTheStream: Source[DurableStateChange[String], NotUsed] =
      store.currentChanges("tag-1", NoOffset)
    // #current-changes
  }

  def changes(): Unit = {
    implicit val system: ActorSystem = ActorSystem()

    // #changes
    import org.apache.pekko
    import pekko.NotUsed
    import pekko.stream.scaladsl.Source
    import pekko.persistence.state.DurableStateStoreRegistry
    import pekko.persistence.jdbc.state.scaladsl.JdbcDurableStateStore
    import pekko.persistence.query.{ DurableStateChange, NoOffset }

    val store = DurableStateStoreRegistry
      .get(system)
      .durableStateStoreFor[JdbcDurableStateStore[String]](JdbcDurableStateStore.Identifier)

    val willNotCompleteTheStream: Source[DurableStateChange[String], NotUsed] =
      store.changes("tag-1", NoOffset)
    // #changes
  }
}
