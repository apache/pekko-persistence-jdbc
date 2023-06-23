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

package org.apache.pekko.persistence.jdbc.snapshot.dao.legacy

import org.apache.pekko
import pekko.persistence.SnapshotMetadata
import pekko.persistence.jdbc.serialization.SnapshotSerializer
import pekko.persistence.jdbc.snapshot.dao.legacy.SnapshotTables.SnapshotRow
import pekko.persistence.serialization.Snapshot
import pekko.serialization.Serialization

import scala.util.Try

class ByteArraySnapshotSerializer(serialization: Serialization) extends SnapshotSerializer[SnapshotRow] {
  def serialize(metadata: SnapshotMetadata, snapshot: Any): Try[SnapshotRow] = {
    serialization
      .serialize(Snapshot(snapshot))
      .map(SnapshotRow(metadata.persistenceId, metadata.sequenceNr, metadata.timestamp, _))
  }

  def deserialize(snapshotRow: SnapshotRow): Try[(SnapshotMetadata, Any)] = {
    serialization
      .deserialize(snapshotRow.snapshot, classOf[Snapshot])
      .map(snapshot => {
        val snapshotMetadata =
          SnapshotMetadata(snapshotRow.persistenceId, snapshotRow.sequenceNumber, snapshotRow.created)
        (snapshotMetadata, snapshot.data)
      })
  }
}
