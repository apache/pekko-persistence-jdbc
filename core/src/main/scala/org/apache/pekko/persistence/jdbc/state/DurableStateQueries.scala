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

package org.apache.pekko.persistence.jdbc.state

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.persistence.jdbc.config.DurableStateTableConfiguration
import pekko.persistence.jdbc.db.MariaDBProfile

import slick.jdbc.{
  H2Profile,
  JdbcProfile,
  MySQLProfile,
  OracleProfile,
  PostgresProfile,
  SQLServerProfile,
  SetParameter
}

/**
 * INTERNAL API
 */
@InternalApi private[pekko] class DurableStateQueries(
    val profile: JdbcProfile,
    override val durableStateTableCfg: DurableStateTableConfiguration)
    extends DurableStateTables {

  import profile.api._

  lazy val sequenceNextValUpdater = profile match {
    case H2Profile        => new H2SequenceNextValUpdater(profile, durableStateTableCfg)
    case PostgresProfile  => new PostgresSequenceNextValUpdater(profile, durableStateTableCfg)
    case SQLServerProfile => new SqlServerSequenceNextValUpdater(profile, durableStateTableCfg)
    case OracleProfile    => new OracleSequenceNextValUpdater(profile, durableStateTableCfg)
    case MySQLProfile     => new MySQLSequenceNextValUpdater(profile, durableStateTableCfg)
    case MariaDBProfile   => new MariaDBSequenceNextValUpdater(profile, durableStateTableCfg)
    case _                => throw new UnsupportedOperationException(s"Unsupported JdbcProfile <$profile> for durableState.")
  }

  implicit val uuidSetter: SetParameter[Array[Byte]] = SetParameter[Array[Byte]] { case (bytes, params) =>
    params.setBytes(bytes)
  }

  private[jdbc] def selectFromDbByPersistenceId(persistenceId: Rep[String]) =
    durableStateTable.filter(_.persistenceId === persistenceId)

  private[jdbc] def insertDbWithDurableState(row: DurableStateTables.DurableStateRow, seqNextValue: String) = {
    sqlu"""INSERT INTO #${durableStateTableCfg.schemaAndTableName}
            (
             #${durableStateTableCfg.columnNames.persistenceId},
             #${durableStateTableCfg.columnNames.globalOffset},
             #${durableStateTableCfg.columnNames.revision},
             #${durableStateTableCfg.columnNames.statePayload},
             #${durableStateTableCfg.columnNames.stateSerId},
             #${durableStateTableCfg.columnNames.stateSerManifest},
             #${durableStateTableCfg.columnNames.tag},
             #${durableStateTableCfg.columnNames.stateTimestamp}
            )
            VALUES
            (
              ${row.persistenceId},
              #$seqNextValue,
              ${row.revision},
              ${row.statePayload},
              ${row.stateSerId},
              ${row.stateSerManifest},
              ${row.tag},
              #${System.currentTimeMillis()}
            )
      """
  }

  private[jdbc] def updateDbWithDurableState(row: DurableStateTables.DurableStateRow, seqNextValue: String) = {
    sqlu"""UPDATE #${durableStateTableCfg.schemaAndTableName}
           SET #${durableStateTableCfg.columnNames.globalOffset} = #$seqNextValue,
               #${durableStateTableCfg.columnNames.revision} = ${row.revision},
               #${durableStateTableCfg.columnNames.statePayload} = ${row.statePayload},
               #${durableStateTableCfg.columnNames.stateSerId} = ${row.stateSerId},
               #${durableStateTableCfg.columnNames.stateSerManifest} = ${row.stateSerManifest},
               #${durableStateTableCfg.columnNames.tag} = ${row.tag},
               #${durableStateTableCfg.columnNames.stateTimestamp} = ${System.currentTimeMillis}
           WHERE #${durableStateTableCfg.columnNames.persistenceId} = ${row.persistenceId}
             AND #${durableStateTableCfg.columnNames.revision} = ${row.revision} - 1
        """
  }

  private def replaceDbGlobalOffsetTable() = {
    sqlu"""REPLACE INTO #${durableStateTableCfg.globalOffsetTableName} (dummy) VALUES (0)"""
  }

  private[jdbc] def getSequenceNextValueExpr() = profile match {
    case MySQLProfile =>
      replaceDbGlobalOffsetTable()
        .andThen(sequenceNextValUpdater.getSequenceNextValueExpr())
        .transactionally
    case _ =>
      sequenceNextValUpdater.getSequenceNextValueExpr()
  }

  def deleteFromDb(persistenceId: String) = {
    durableStateTable.filter(_.persistenceId === persistenceId).delete
  }

  /**
   * Deletes a particular revision of an object based on its persistenceId.
   * This revision may no longer exist and if so, no delete will occur.
   *
   * @since 1.1.0
   */
  private[jdbc] def deleteBasedOnPersistenceIdAndRevision(persistenceId: String, revision: Long) = {
    selectFromDbByPersistenceId(persistenceId).filter(_.revision === revision).delete
  }

  def deleteAllFromDb() = {
    durableStateTable.delete
  }

  private[jdbc] val maxOffsetQuery = Compiled {
    durableStateTable.map(_.globalOffset).max.getOrElse(0L)
  }

  private def _changesByTag(
      tag: Rep[String],
      offset: ConstColumn[Long],
      maxOffset: ConstColumn[Long],
      max: ConstColumn[Long]) = {
    durableStateTable
      .filter(_.tag === tag)
      .sortBy(_.globalOffset.asc)
      .filter(row => row.globalOffset > offset && row.globalOffset <= maxOffset)
      .take(max)
  }

  private[jdbc] val changesByTag = Compiled(_changesByTag _)

  private def _stateStoreStateQuery(from: ConstColumn[Long], limit: ConstColumn[Long]) =
    durableStateTable // FIXME change this to a specialized query to only retrieve the 3 columns of interest
      .filter(_.globalOffset > from)
      .sortBy(_.globalOffset.asc)
      .take(limit)
      .map(s => (s.persistenceId, s.globalOffset, s.revision))

  val stateStoreStateQuery = Compiled(_stateStoreStateQuery _)
}
