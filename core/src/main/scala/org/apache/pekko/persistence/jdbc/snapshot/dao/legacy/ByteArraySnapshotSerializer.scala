/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.snapshot.dao.legacy

import org.apache.pekko.persistence.SnapshotMetadata
import org.apache.pekko.persistence.jdbc.serialization.SnapshotSerializer
import org.apache.pekko.persistence.jdbc.snapshot.dao.legacy.SnapshotTables.SnapshotRow
import org.apache.pekko.persistence.serialization.Snapshot
import org.apache.pekko.serialization.Serialization

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
