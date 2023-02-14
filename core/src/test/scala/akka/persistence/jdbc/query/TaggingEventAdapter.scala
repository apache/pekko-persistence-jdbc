/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.query

import org.apache.pekko.persistence.jdbc.query.TaggingEventAdapter.TagEvent
import org.apache.pekko.persistence.journal.{ Tagged, WriteEventAdapter }

object TaggingEventAdapter {
  case class TagEvent(payload: Any, tags: Set[String])
}

/**
 * The TaggingEventAdapter will instruct persistence
 * to tag the received event.
 */
class TaggingEventAdapter extends WriteEventAdapter {
  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any =
    event match {
      case TagEvent(payload, tags) =>
        Tagged(payload, tags)
      case _ => event
    }
}
