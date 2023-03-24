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

package org.apache.pekko.persistence.jdbc.snapshot.dao

import slick.jdbc.{ JdbcBackend, JdbcProfile }
import org.apache.pekko
import pekko.persistence.SnapshotMetadata
import pekko.persistence.jdbc.config.SnapshotConfig
import pekko.serialization.Serialization
import pekko.stream.Materializer
import SnapshotTables._
import pekko.dispatch.ExecutionContexts
import pekko.persistence.jdbc.PekkoSerialization

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Success, Try }

class DefaultSnapshotDao(
    db: JdbcBackend#Database,
    profile: JdbcProfile,
    snapshotConfig: SnapshotConfig,
    serialization: Serialization)(implicit ec: ExecutionContext, val mat: Materializer)
    extends SnapshotDao {
  import profile.api._
  val queries = new SnapshotQueries(profile, snapshotConfig.snapshotTableConfiguration)

  private def toSnapshotData(row: SnapshotRow): Try[(SnapshotMetadata, Any)] = {
    val snapshot = serialization.deserialize(row.snapshotPayload, row.snapshotSerId, row.snapshotSerManifest)

    snapshot.flatMap { snapshot =>
      val metadata = for {
        mPayload <- row.metaPayload
        mSerId <- row.metaSerId
      } yield (mPayload, mSerId)

      metadata match {
        case None =>
          Success((SnapshotMetadata(row.persistenceId, row.sequenceNumber, row.created), snapshot))
        case Some((payload, id)) =>
          serialization.deserialize(payload, id, row.metaSerManifest.getOrElse("")).map { meta =>
            (SnapshotMetadata(row.persistenceId, row.sequenceNumber, row.created, Some(meta)), snapshot)
          }
      }
    }
  }

  private def serializeSnapshot(meta: SnapshotMetadata, snapshot: Any): Try[SnapshotRow] = {
    val serializedMetadata = meta.metadata.flatMap(m => PekkoSerialization.serialize(serialization, m).toOption)
    PekkoSerialization
      .serialize(serialization, payload = snapshot)
      .map(serializedSnapshot =>
        SnapshotRow(
          meta.persistenceId,
          meta.sequenceNr,
          meta.timestamp,
          serializedSnapshot.serId,
          serializedSnapshot.serManifest,
          serializedSnapshot.payload,
          serializedMetadata.map(_.serId),
          serializedMetadata.map(_.serManifest),
          serializedMetadata.map(_.payload)))
  }

  private def zeroOrOneSnapshot(rows: Seq[SnapshotRow]): Option[(SnapshotMetadata, Any)] =
    rows.headOption.map(row => toSnapshotData(row).get) // throw is from a future map

  override def latestSnapshot(persistenceId: String): Future[Option[(SnapshotMetadata, Any)]] =
    db.run(queries.selectLatestByPersistenceId(persistenceId).result).flatMap { rows =>
      rows.headOption match {
        case Some(row) => Future.fromTry(toSnapshotData(row)).map(Option(_))
        case None      => Future.successful(None)
      }
    }

  override def snapshotForMaxTimestamp(
      persistenceId: String,
      maxTimestamp: Long): Future[Option[(SnapshotMetadata, Any)]] =
    db.run(queries.selectOneByPersistenceIdAndMaxTimestamp((persistenceId, maxTimestamp)).result).map(zeroOrOneSnapshot)

  override def snapshotForMaxSequenceNr(
      persistenceId: String,
      maxSequenceNr: Long): Future[Option[(SnapshotMetadata, Any)]] =
    db.run(queries.selectOneByPersistenceIdAndMaxSequenceNr((persistenceId, maxSequenceNr)).result)
      .map(zeroOrOneSnapshot)

  override def snapshotForMaxSequenceNrAndMaxTimestamp(
      persistenceId: String,
      maxSequenceNr: Long,
      maxTimestamp: Long): Future[Option[(SnapshotMetadata, Any)]] =
    db.run(
      queries
        .selectOneByPersistenceIdAndMaxSequenceNrAndMaxTimestamp((persistenceId, maxSequenceNr, maxTimestamp))
        .result)
      .map(zeroOrOneSnapshot(_))

  override def save(snapshotMetadata: SnapshotMetadata, snapshot: Any): Future[Unit] = {
    val eventualSnapshotRow = Future.fromTry(serializeSnapshot(snapshotMetadata, snapshot))
    eventualSnapshotRow.map(queries.insertOrUpdate).flatMap(db.run).map(_ => ())(ExecutionContexts.parasitic)
  }

  override def delete(persistenceId: String, sequenceNr: Long): Future[Unit] =
    db.run(queries.selectByPersistenceIdAndSequenceNr((persistenceId, sequenceNr)).delete)
      .map(_ => ())(ExecutionContexts.parasitic)

  override def deleteAllSnapshots(persistenceId: String): Future[Unit] =
    db.run(queries.selectAll(persistenceId).delete).map(_ => ())(ExecutionContexts.parasitic)

  override def deleteUpToMaxSequenceNr(persistenceId: String, maxSequenceNr: Long): Future[Unit] =
    db.run(queries.selectByPersistenceIdUpToMaxSequenceNr((persistenceId, maxSequenceNr)).delete)
      .map(_ => ())(ExecutionContexts.parasitic)

  override def deleteUpToMaxTimestamp(persistenceId: String, maxTimestamp: Long): Future[Unit] =
    db.run(queries.selectByPersistenceIdUpToMaxTimestamp((persistenceId, maxTimestamp)).delete)
      .map(_ => ())(ExecutionContexts.parasitic)

  override def deleteUpToMaxSequenceNrAndMaxTimestamp(
      persistenceId: String,
      maxSequenceNr: Long,
      maxTimestamp: Long): Future[Unit] =
    db.run(
      queries
        .selectByPersistenceIdUpToMaxSequenceNrAndMaxTimestamp((persistenceId, maxSequenceNr, maxTimestamp))
        .delete)
      .map(_ => ())(ExecutionContexts.parasitic)
}
