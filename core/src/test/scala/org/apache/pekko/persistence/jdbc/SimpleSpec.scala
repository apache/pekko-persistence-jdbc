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

package org.apache.pekko.persistence.jdbc

import org.apache.pekko
import pekko.actor.{ ActorRef, ActorSystem }
import pekko.persistence.jdbc.util.ClasspathResources
import pekko.testkit.TestProbe
import org.scalatest._
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

trait SimpleSpec
    extends AnyFlatSpec
    with Matchers
    with ScalaFutures
    with TryValues
    with OptionValues
    with Eventually
    with ClasspathResources
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with GivenWhenThen {

  /**
   * Sends the PoisonPill command to an actor and waits for it to die
   */
  def killActors(actors: ActorRef*)(implicit system: ActorSystem): Unit = {
    val tp = TestProbe()
    actors.foreach { (actor: ActorRef) =>
      tp.watch(actor)
      system.stop(actor)
      tp.expectTerminated(actor)
    }
  }
}
