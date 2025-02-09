/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.query.javadsl

import java.util.Optional
import java.util.concurrent.CompletionStage

/**
 * A trait that enables querying the current last known sequence number for a given `persistenceId`.
 */
trait CurrentLastKnownSequenceNumberByPersistenceIdQuery {

  /**
   * Returns the last known sequence number for the given `persistenceId`. Empty if the `persistenceId` is unknown.
   *
   * @param persistenceId The `persistenceId` for which the last known sequence number should be returned.
   * @return Some sequence number or None if the `persistenceId` is unknown.
   */
  def currentLastKnownSequenceNumberByPersistenceId(persistenceId: String): CompletionStage[Optional[Long]]
}
