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

package org.apache.pekko.persistence.jdbc
package journal.dao.legacy

import org.apache.pekko
import pekko.persistence.PersistentRepr
import pekko.persistence.jdbc.serialization.FlowPersistentReprSerializer
import pekko.serialization.Serialization

import scala.collection.immutable._
import scala.util.Try

class ByteArrayJournalSerializer(serialization: Serialization, separator: String)
    extends FlowPersistentReprSerializer[JournalRow] {
  override def serialize(persistentRepr: PersistentRepr, tags: Set[String]): Try[JournalRow] = {
    serialization
      .serialize(persistentRepr)
      .map(
        JournalRow(
          Long.MinValue,
          persistentRepr.deleted,
          persistentRepr.persistenceId,
          persistentRepr.sequenceNr,
          _,
          encodeTags(tags, separator)))
  }

  override def deserialize(journalRow: JournalRow): Try[(PersistentRepr, Set[String], Long)] = {
    serialization
      .deserialize(journalRow.message, classOf[PersistentRepr])
      .map((_, decodeTags(journalRow.tags, separator), journalRow.ordering))
  }
}
