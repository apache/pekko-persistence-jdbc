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

package org.apache.pekko.persistence.jdbc.util

import scala.collection.immutable._
import scala.util.{ Failure, Success, Try }

object TrySeq {
  def sequence[A](seq: Seq[Try[A]]): Try[Seq[A]] = {
    def recurse(remaining: Seq[Try[A]], processed: Seq[A]): Try[Seq[A]] =
      remaining match {
        case Seq()                 => Success(processed)
        case Success(head) +: tail => recurse(remaining = tail, processed :+ head)
        case Failure(t) +: _       => Failure(t)
      }
    recurse(seq, Vector.empty)
  }
}
