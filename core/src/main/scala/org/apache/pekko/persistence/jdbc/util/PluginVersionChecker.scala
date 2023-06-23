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

import org.apache.pekko.annotation.InternalApi

@InternalApi
private[jdbc] object PluginVersionChecker {
  def check(): Unit =
    try {
      Class.forName("org.apache.pekko.persistence.jdbc.util.DefaultSlickDatabaseProvider")
      throw new RuntimeException(
        "Old version of Apache Pekko Persistence JDBC found on the classpath. Remove `com.github.dnvriend:pekko-persistence-jdbc` from the classpath..")
    } catch {
      case _: ClassNotFoundException =>
      // All good! That's intentional.
      // It's good if we don't have pekko.persistence.jdbc.util.DefaultSlickDatabaseProvider around
    }
}
