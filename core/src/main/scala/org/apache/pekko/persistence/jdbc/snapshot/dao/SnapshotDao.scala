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

import java.util.concurrent.ConcurrentHashMap

import org.apache.pekko.persistence.{ SnapshotMetadata, SnapshotSelectionCriteria }
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }

private object SnapshotDaoWarnings {
  private val warnedDaoClasses = ConcurrentHashMap.newKeySet[String]()

  def ignoredMinimumBounds(dao: SnapshotDao, criteria: SnapshotSelectionCriteria): Unit = {
    val daoClass = dao.getClass.getName
    if ((criteria.minSequenceNr != 0L || criteria.minTimestamp != 0L) && warnedDaoClasses.add(daoClass))
      LoggerFactory.getLogger(dao.getClass).warn(
        "Snapshot DAO [{}] uses the compatibility deleteByCriteria implementation; minimum bounds are ignored. " +
        "Override deleteByCriteria to apply all criteria bounds.",
        daoClass)
  }
}

trait SnapshotDao {

  /**
   * Load the snapshot with the highest sequence number matching all inclusive criteria bounds.
   * The default delegates to the existing upper-bound methods for custom DAO compatibility,
   * then filters the returned snapshot against all bounds. Custom DAOs should override this
   * method to query all four bounds directly, as the built-in DAOs do; otherwise a snapshot
   * matching the criteria may be missed when timestamps are not ordered by sequence number.
   * Subclasses of a built-in DAO that override upper-bound loading methods are bypassed by
   * the built-in implementation and must also override this method to apply their behavior.
   */
  def snapshotForCriteria(
      persistenceId: String,
      criteria: SnapshotSelectionCriteria): Future[Option[(SnapshotMetadata, Any)]] = {
    val snapshot = criteria match {
      case SnapshotSelectionCriteria(Long.MaxValue, Long.MaxValue, _, _) =>
        latestSnapshot(persistenceId)
      case SnapshotSelectionCriteria(Long.MaxValue, maxTimestamp, _, _) =>
        snapshotForMaxTimestamp(persistenceId, maxTimestamp)
      case SnapshotSelectionCriteria(maxSequenceNr, Long.MaxValue, _, _) =>
        snapshotForMaxSequenceNr(persistenceId, maxSequenceNr)
      case SnapshotSelectionCriteria(maxSequenceNr, maxTimestamp, _, _) =>
        snapshotForMaxSequenceNrAndMaxTimestamp(persistenceId, maxSequenceNr, maxTimestamp)
    }
    snapshot.map(_.filter { case (metadata, _) => criteria.matches(metadata) })(ExecutionContext.parasitic)
  }

  /**
   * Delete only snapshots matching all inclusive criteria bounds for this persistence ID.
   * The default delegates to the existing upper-bound methods for custom DAO compatibility,
   * ignoring minimum bounds. Custom DAOs must override this method to apply all four bounds,
   * as the built-in DAOs do; otherwise snapshots below a minimum bound may also be deleted.
   * Subclasses of a built-in DAO that override upper-bound deletion methods are bypassed by
   * the built-in implementation and must also override this method to apply their behavior.
   */
  def deleteByCriteria(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Unit] = {
    SnapshotDaoWarnings.ignoredMinimumBounds(this, criteria)

    criteria match {
      case SnapshotSelectionCriteria(Long.MaxValue, Long.MaxValue, _, _) =>
        deleteAllSnapshots(persistenceId)
      case SnapshotSelectionCriteria(Long.MaxValue, maxTimestamp, _, _) =>
        deleteUpToMaxTimestamp(persistenceId, maxTimestamp)
      case SnapshotSelectionCriteria(maxSequenceNr, Long.MaxValue, _, _) =>
        deleteUpToMaxSequenceNr(persistenceId, maxSequenceNr)
      case SnapshotSelectionCriteria(maxSequenceNr, maxTimestamp, _, _) =>
        deleteUpToMaxSequenceNrAndMaxTimestamp(persistenceId, maxSequenceNr, maxTimestamp)
    }
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
