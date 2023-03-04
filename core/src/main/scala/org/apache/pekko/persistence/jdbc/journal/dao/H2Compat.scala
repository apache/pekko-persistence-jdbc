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

import slick.jdbc.JdbcProfile

trait H2Compat {

  val profile: JdbcProfile

  private lazy val isH2Driver = profile match {
    case slick.jdbc.H2Profile => true
    case _                    => false
  }

  def correctMaxForH2Driver(max: Long): Long = {
    if (isH2Driver) {
      Math.min(max, Int.MaxValue) // H2 only accepts a LIMIT clause as an Integer
    } else {
      max
    }
  }
}
