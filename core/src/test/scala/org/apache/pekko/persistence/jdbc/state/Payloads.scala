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

package org.apache.pekko.persistence.jdbc.state

import org.apache.pekko.serialization._

final case class MyPayload(data: String)

class MyPayloadSerializer extends Serializer {
  val MyPayloadClass = classOf[MyPayload]

  def identifier: Int = 77123
  def includeManifest: Boolean = true

  def toBinary(o: AnyRef): Array[Byte] = o match {
    case MyPayload(data) => s"$data".getBytes("UTF-8")
    case _               => throw new Exception("Unknown object for serialization")
  }

  def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = manifest match {
    case Some(MyPayloadClass) => MyPayload(s"${new String(bytes, "UTF-8")}")
    case Some(c)              => throw new Exception(s"unexpected manifest $c")
    case None                 => throw new Exception("no manifest")
  }
}
