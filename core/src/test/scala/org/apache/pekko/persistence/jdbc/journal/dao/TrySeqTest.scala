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

package org.apache.pekko.persistence.jdbc.journal.dao

import org.apache.pekko
import pekko.persistence.jdbc.util.TrySeq
import pekko.persistence.jdbc.SimpleSpec

import scala.collection.immutable._
import scala.util.{ Failure, Success }

class TrySeqTest extends SimpleSpec {
  def failure(text: String) = Failure(new RuntimeException(text))

  it should "sequence an empty immutable.Seq" in {
    TrySeq.sequence(Seq.empty) shouldBe Success(Seq.empty)
  }

  it should "sequence an empty immutable.Vector" in {
    TrySeq.sequence(Vector.empty) shouldBe Success(Seq.empty)
  }

  it should "sequence a immutable.Seq of success/success" in {
    TrySeq.sequence(Seq(Success("a"), Success("b"))) shouldBe Success(Seq("a", "b"))
  }

  it should "sequence an immutable Seq of success/failure" in {
    val result = TrySeq.sequence(List(Success("a"), failure("b")))
    result should matchPattern { case Failure(cause) if cause.getMessage.contains("b") => }
  }

  it should "sequence an immutable Seq of failure/success" in {
    val result = TrySeq.sequence(List(failure("a"), Success("b")))
    result should matchPattern { case Failure(cause) if cause.getMessage.contains("a") => }
  }

  it should "sequence an immutable.Seq of failure/failure" in {
    val result = TrySeq.sequence(Seq(failure("a"), failure("b")))
    result should matchPattern { case Failure(cause) if cause.getMessage.contains("a") => }
  }

  it should "sequence an immutable.Vector of failure/failure" in {
    val result = TrySeq.sequence(Vector(failure("a"), failure("b")))
    result should matchPattern { case Failure(cause) if cause.getMessage.contains("a") => }
  }
}
