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
import slick.jdbc.JdbcProfile
import slick.dbio.Effect
import slick.sql.SqlStreamingAction

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] trait SequenceNextValUpdater {
  def getSequenceNextValueExpr(): SqlStreamingAction[Vector[String], String, Effect]
}

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] final class H2SequenceNextValUpdater(
    profile: JdbcProfile, durableStateTableCfg: DurableStateTableConfiguration)
    extends SequenceNextValUpdater {

  import profile.api._

  // H2 dependent (https://stackoverflow.com/questions/36244641/h2-equivalent-of-postgres-serial-or-bigserial-column)
  def getSequenceNextValueExpr() = {
    sql"""SELECT COLUMN_DEFAULT
          FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_NAME = '#${durableStateTableCfg.tableName}'
            AND COLUMN_NAME = '#${durableStateTableCfg.columnNames.globalOffset}'
            AND TABLE_SCHEMA = 'PUBLIC'""".as[String]
  }
}

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] final class PostgresSequenceNextValUpdater(
    profile: JdbcProfile, durableStateTableCfg: DurableStateTableConfiguration)
    extends SequenceNextValUpdater {

  import profile.api._

  def getSequenceNextValueExpr() =
    sql"""SELECT nextval(pg_get_serial_sequence('#${durableStateTableCfg.schemaAndTableName}', '#${durableStateTableCfg
        .columnNames.globalOffset}'))""".as[
      String]
}

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] final class SqlServerSequenceNextValUpdater(profile: JdbcProfile,
    durableStateTableCfg: DurableStateTableConfiguration)
    extends SequenceNextValUpdater {

  import profile.api._

  def getSequenceNextValueExpr() =
    sql"""SELECT NEXT VALUE FOR #${durableStateTableCfg.columnNames.globalOffset}""".as[String]
}

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] final class OracleSequenceNextValUpdater(profile: JdbcProfile,
    durableStateTableCfg: DurableStateTableConfiguration)
    extends SequenceNextValUpdater {

  import profile.api._

  def getSequenceNextValueExpr() =
    sql"""SELECT #${durableStateTableCfg.tableName}__#${durableStateTableCfg.columnNames
        .globalOffset}_SEQ.nextval FROM DUAL""".as[
      String]
}

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] final class MySQLSequenceNextValUpdater(profile: JdbcProfile,
    durableStateTableCfg: DurableStateTableConfiguration)
    extends SequenceNextValUpdater {

  import profile.api._

  def getSequenceNextValueExpr() = if (durableStateTableCfg.useExplicitSelectForGlobalOffset) {
    sql"""SELECT #${durableStateTableCfg.columnNames.globalOffsetValue} FROM #${durableStateTableCfg.globalOffsetTableName}""".as[
      String]
  } else {
    sql"""SELECT LAST_INSERT_ID()""".as[String]
  }
}

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] final class MariaDBSequenceNextValUpdater(profile: JdbcProfile,
    durableStateTableCfg: DurableStateTableConfiguration)
    extends SequenceNextValUpdater {

  import profile.api._

  def getSequenceNextValueExpr() =
    sql"""SELECT NEXT VALUE FOR #${durableStateTableCfg.globalOffsetSequenceName}""".as[String]
}
