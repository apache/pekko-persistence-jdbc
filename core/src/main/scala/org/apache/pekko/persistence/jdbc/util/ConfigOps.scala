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

import com.typesafe.config.Config

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object ConfigOps {

  implicit class ConfigOperations(val config: Config) extends AnyVal {
    def asStringOption(key: String): Option[String] =
      if (config.hasPath(key)) {
        val value = config.getString(key).trim
        if (value.isEmpty) None
        else Some(value)
      } else None

    def asFiniteDuration(key: String): FiniteDuration =
      FiniteDuration(config.getDuration(key).toMillis, TimeUnit.MILLISECONDS)

  }
}
