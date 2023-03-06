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

package org.apache.pekko.persistence.jdbc.util

import scala.concurrent.duration.{ FiniteDuration, _ }
import scala.concurrent.{ Await, Future }

object BlockingOps {
  implicit class BlockingFutureImplicits[T](val that: Future[T]) extends AnyVal {
    def futureValue(implicit awaitDuration: FiniteDuration = 24.hour): T =
      Await.result(that, awaitDuration)
    def printFutureValue(implicit awaitDuration: FiniteDuration = 24.hour): Unit =
      println(that.futureValue)
  }
}
