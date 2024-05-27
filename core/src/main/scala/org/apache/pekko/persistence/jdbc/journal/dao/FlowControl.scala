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

package org.apache.pekko.persistence.jdbc.journal.dao

private[jdbc] sealed trait FlowControl

private[jdbc] object FlowControl {

  /** Keep querying - used when we are sure that there is more events to fetch */
  case object Continue extends FlowControl

  /**
   * Keep querying with delay - used when we have consumed all events,
   * but want to poll for future events
   */
  case object ContinueDelayed extends FlowControl

  /**
   * if the limited windows unable query anything, then fallback to full windows.
   */
  case object Fallback extends FlowControl

  /** Stop querying - used when we reach the desired offset */
  case object Stop extends FlowControl
}
