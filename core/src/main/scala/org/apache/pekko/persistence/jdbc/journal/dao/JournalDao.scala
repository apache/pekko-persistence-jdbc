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

import org.apache.pekko.persistence.AtomicWrite
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.Try

trait JournalDao extends JournalDaoWithReadMessages {

  /**
   * Deletes all persistent messages up to toSequenceNr (inclusive) for the persistenceId
   */
  def delete(persistenceId: String, toSequenceNr: Long): Future[Unit]

  /**
   * Returns the highest sequence number for the events that are stored for that `persistenceId`. When no events are
   * found for the `persistenceId`, 0L will be the highest sequence number
   */
  def highestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long]

  /**
   * @see [[pekko.persistence.journal.AsyncWriteJournal.asyncWriteMessages(messages)]]
   */
  def asyncWriteMessages(messages: Seq[AtomicWrite]): Future[Seq[Try[Unit]]]
}
