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

package org.apache.pekko.persistence.jdbc.snapshot.dao

import org.apache.pekko.persistence.{ SnapshotMetadata, SnapshotSelectionCriteria }

import scala.concurrent.Future

trait SnapshotDao {

  /**
   * Load the snapshot with the highest sequence number matching all inclusive criteria bounds.
   * The default preserves upper-bound-only queries. Custom DAOs must override this method to
   * support nonzero minimum bounds; otherwise the returned future fails explicitly.
   */
  def snapshotForCriteria(
      persistenceId: String,
      criteria: SnapshotSelectionCriteria): Future[Option[(SnapshotMetadata, Any)]] =
    criteria match {
      case SnapshotSelectionCriteria(Long.MaxValue, Long.MaxValue, 0L, 0L) =>
        latestSnapshot(persistenceId)
      case SnapshotSelectionCriteria(Long.MaxValue, maxTimestamp, 0L, 0L) =>
        snapshotForMaxTimestamp(persistenceId, maxTimestamp)
      case SnapshotSelectionCriteria(maxSequenceNr, Long.MaxValue, 0L, 0L) =>
        snapshotForMaxSequenceNr(persistenceId, maxSequenceNr)
      case SnapshotSelectionCriteria(maxSequenceNr, maxTimestamp, 0L, 0L) =>
        snapshotForMaxSequenceNrAndMaxTimestamp(persistenceId, maxSequenceNr, maxTimestamp)
      case _ =>
        Future.failed(new UnsupportedOperationException(
          "SnapshotDao must override snapshotForCriteria to support nonzero minimum bounds"))
    }

  /**
   * Delete only snapshots matching all inclusive criteria bounds for this persistence ID.
   * The default preserves upper-bound-only deletes and fails for nonzero minimum bounds.
   * Custom DAOs must override this method to support bounded deletion without widening its range.
   */
  def deleteByCriteria(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Unit] =
    criteria match {
      case SnapshotSelectionCriteria(Long.MaxValue, Long.MaxValue, 0L, 0L) =>
        deleteAllSnapshots(persistenceId)
      case SnapshotSelectionCriteria(Long.MaxValue, maxTimestamp, 0L, 0L) =>
        deleteUpToMaxTimestamp(persistenceId, maxTimestamp)
      case SnapshotSelectionCriteria(maxSequenceNr, Long.MaxValue, 0L, 0L) =>
        deleteUpToMaxSequenceNr(persistenceId, maxSequenceNr)
      case SnapshotSelectionCriteria(maxSequenceNr, maxTimestamp, 0L, 0L) =>
        deleteUpToMaxSequenceNrAndMaxTimestamp(persistenceId, maxSequenceNr, maxTimestamp)
      case _ =>
        Future.failed(new UnsupportedOperationException(
          "SnapshotDao must override deleteByCriteria to support nonzero minimum bounds"))
    }

  def deleteAllSnapshots(persistenceId: String): Future[Unit]

  def deleteUpToMaxSequenceNr(persistenceId: String, maxSequenceNr: Long): Future[Unit]

  def deleteUpToMaxTimestamp(persistenceId: String, maxTimestamp: Long): Future[Unit]

  def deleteUpToMaxSequenceNrAndMaxTimestamp(
      persistenceId: String,
      maxSequenceNr: Long,
      maxTimestamp: Long): Future[Unit]

  def latestSnapshot(persistenceId: String): Future[Option[(SnapshotMetadata, Any)]]

  def snapshotForMaxTimestamp(persistenceId: String, timestamp: Long): Future[Option[(SnapshotMetadata, Any)]]

  def snapshotForMaxSequenceNr(persistenceId: String, sequenceNr: Long): Future[Option[(SnapshotMetadata, Any)]]

  def snapshotForMaxSequenceNrAndMaxTimestamp(
      persistenceId: String,
      sequenceNr: Long,
      timestamp: Long): Future[Option[(SnapshotMetadata, Any)]]

  def delete(persistenceId: String, sequenceNr: Long): Future[Unit]

  def save(snapshotMetadata: SnapshotMetadata, snapshot: Any): Future[Unit]
}
