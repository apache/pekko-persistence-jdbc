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
package journal.dao.legacy

import org.apache.pekko.persistence.{ AtomicWrite, PersistentRepr }

import scala.collection.immutable._

class ByteArrayJournalSerializerTest extends SharedActorSystemTestSpec() {
  it should "serialize a serializable message and indicate whether or not the serialization succeeded" in {
    val serializer = new ByteArrayJournalSerializer(serialization, ",")
    val result = serializer.serialize(Seq(AtomicWrite(PersistentRepr("foo"))))
    result should have size 1
    (result.head should be).a(Symbol("success"))
  }

  it should "not serialize a non-serializable message and indicate whether or not the serialization succeeded" in {
    class Test
    val serializer = new ByteArrayJournalSerializer(serialization, ",")
    val result = serializer.serialize(Seq(AtomicWrite(PersistentRepr(new Test))))
    result should have size 1
    (result.head should be).a(Symbol("failure"))
  }

  it should "serialize non-serializable and serializable messages and indicate whether or not the serialization succeeded" in {
    class Test
    val serializer = new ByteArrayJournalSerializer(serialization, ",")
    val result = serializer.serialize(List(AtomicWrite(PersistentRepr(new Test)), AtomicWrite(PersistentRepr("foo"))))
    result should have size 2
    (result.head should be).a(Symbol("failure"))
    (result.last should be).a(Symbol("success"))
  }
}
