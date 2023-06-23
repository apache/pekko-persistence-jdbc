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

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.Scheduler
import pekko.persistence.PersistentRepr
import pekko.stream.scaladsl.Source

trait JournalDaoWithReadMessages {

  /**
   * Returns a Source of PersistentRepr and ordering number for a certain persistenceId.
   * It includes the events with sequenceNr between `fromSequenceNr` (inclusive) and
   * `toSequenceNr` (inclusive).
   */
  def messages(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      max: Long): Source[Try[(PersistentRepr, Long)], NotUsed]

  /**
   * Returns a Source of PersistentRepr and ordering number for a certain persistenceId.
   * It includes the events with sequenceNr between `fromSequenceNr` (inclusive) and
   * `toSequenceNr` (inclusive).
   */
  def messagesWithBatch(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      batchSize: Int,
      refreshInterval: Option[(FiniteDuration, Scheduler)]): Source[Try[(PersistentRepr, Long)], NotUsed]

}
