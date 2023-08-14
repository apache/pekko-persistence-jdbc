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

import scala.concurrent.Future
import scala.concurrent.duration._
import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ExtendedActorSystem
import pekko.pattern.ask
import pekko.persistence.jdbc.config.DurableStateTableConfiguration
import pekko.persistence.query.DurableStateChange
import pekko.persistence.query.Offset
import pekko.persistence.state.scaladsl.GetObjectResult
import pekko.stream.scaladsl.Source
import pekko.testkit.TestProbe
import pekko.util.Timeout
import slick.jdbc.{ JdbcBackend, JdbcProfile }
import pekko.serialization.Serialization

object TestProbeDurableStateStoreQuery {
  case class StateInfoSequence(offset: Long, limit: Long)
}

class TestProbeDurableStateStoreQuery(
    val probe: TestProbe,
    db: JdbcBackend#Database,
    profile: JdbcProfile,
    durableStateConfig: DurableStateTableConfiguration,
    serialization: Serialization)(override implicit val system: ExtendedActorSystem)
    extends JdbcDurableStateStore[String](db, profile, durableStateConfig, serialization)(system) {

  implicit val askTimeout: Timeout = Timeout(100.millis)

  override def getObject(persistenceId: String): Future[GetObjectResult[String]] = ???
  override def currentChanges(tag: String, offset: Offset): Source[DurableStateChange[String], NotUsed] = ???

  override def changes(tag: String, offset: Offset): Source[DurableStateChange[String], NotUsed] = ???

  override def stateStoreStateInfo(offset: Long, limit: Long): Source[(String, Long, Long), NotUsed] = {
    val f = probe.ref
      .ask(TestProbeDurableStateStoreQuery.StateInfoSequence(offset, limit))
      .mapTo[scala.collection.immutable.Seq[DurableStateSequenceActor.VisitedElement]]

    Source.future(f).mapConcat(e => e.map(x => (x.pid, x.offset, x.revision)))
  }

  override def maxStateStoreOffset(): Future[Long] = Future.successful(0)
}
