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

package org.apache.pekko.persistence.jdbc.state

import org.apache.pekko.persistence.query._
object OffsetSyntax {
  implicit class OffsetOps(val that: Offset) extends AnyVal {
    def value =
      that match {
        case Sequence(offsetValue) => offsetValue
        case NoOffset              => 0L
        case _                     =>
          throw new IllegalArgumentException(
            "pekko-persistence-jdbc does not support " + that.getClass.getName + " offsets")
      }
  }
}
