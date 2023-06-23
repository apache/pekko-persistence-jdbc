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

import java.io.{ ByteArrayInputStream, InputStream }
import java.util.Base64

object ByteArrayOps {
  implicit class ByteArrayImplicits(val that: Array[Byte]) extends AnyVal {
    def encodeBase64: String = Base64.getEncoder.encodeToString(that)
    def toInputStream: InputStream = new ByteArrayInputStream(that)
  }
}
