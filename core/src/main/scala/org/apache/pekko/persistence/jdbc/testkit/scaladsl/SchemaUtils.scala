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

package org.apache.pekko.persistence.jdbc.testkit.scaladsl

import scala.concurrent.Future

import org.apache.pekko
import pekko.Done
import pekko.actor.ClassicActorSystemProvider
import pekko.annotation.ApiMayChange
import pekko.persistence.jdbc.testkit.internal.SchemaUtilsImpl
import org.slf4j.LoggerFactory

object SchemaUtils {

  private val logger = LoggerFactory.getLogger("org.apache.pekko.persistence.jdbc.testkit.scaladsl.SchemaUtils")

  /**
   * Drops the schema for both the journal and the snapshot table using the default schema definition.
   *
   * For information about the different schemas and supported databases consult
   * https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#database-schema
   *
   * This utility method is intended to be used for testing only.
   * For production, it's recommended to run any DDL statements before the system is started.
   *
   * This method will automatically detects the configured database using the settings from `jdbc-journal` config.
   * If configured with `use-shared-db`, it will use the `pekko-persistence-jdbc.shared-databases` definition instead.
   * See https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#sharing-the-database-connection-pool-between-the-journals for details.
   */
  @ApiMayChange
  def dropIfExists()(implicit actorSystem: ClassicActorSystemProvider): Future[Done] =
    dropIfExists(configKey = "jdbc-journal")

  /**
   * Drops the schema for both the journal and the snapshot table using the default schema definition.
   *
   * For information about the different schemas and supported databases consult
   * https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#database-schema
   *
   * This utility method is intended to be used for testing only.
   * For production, it's recommended to run any DDL statements before the system is started.
   *
   * This method will automatically detects the configured database using the settings from `configKey` config.
   * If configured with `use-shared-db`, it will use the `pekko-persistence-jdbc.shared-databases` definition instead.
   * See https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#sharing-the-database-connection-pool-between-the-journals for details.
   *
   * @param configKey the database journal configuration key to use.
   */
  @ApiMayChange
  def dropIfExists(configKey: String)(implicit actorSystem: ClassicActorSystemProvider): Future[Done] =
    SchemaUtilsImpl.dropIfExists(configKey, logger)

  /**
   * Creates the schema for both the journal and the snapshot table using the default schema definition.
   *
   * For information about the different schemas and supported databases consult
   * https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#database-schema
   *
   * This utility method is intended to be used for testing only.
   * For production, it's recommended to run any DDL statements before the system is started.
   *
   * This method will automatically detects the configured database using the settings from `jdbc-journal` config.
   * If configured with `use-shared-db`, it will use the `pekko-persistence-jdbc.shared-databases` definition instead.
   * See https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#sharing-the-database-connection-pool-between-the-journals for details.
   */
  @ApiMayChange
  def createIfNotExists()(implicit actorSystem: ClassicActorSystemProvider): Future[Done] =
    createIfNotExists(configKey = "jdbc-journal")

  /**
   * Creates the schema for both the journal and the snapshot table using the default schema definition.
   *
   * For information about the different schemas and supported databases consult
   * https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#database-schema
   *
   * This utility method is intended to be used for testing only.
   * For production, it's recommended to run any DDL statements before the system is started.
   *
   * This method will automatically detects the configured database using the settings from `configKey` config.
   * If configured with `use-shared-db`, it will use the `pekko-persistence-jdbc.shared-databases` definition instead.
   * See https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#sharing-the-database-connection-pool-between-the-journals for details.
   *
   * @param configKey the database journal configuration key to use.
   */
  @ApiMayChange
  def createIfNotExists(configKey: String)(implicit actorSystem: ClassicActorSystemProvider): Future[Done] =
    SchemaUtilsImpl.createIfNotExists(configKey, logger)

  /**
   * This method can be used to load alternative DDL scripts.
   *
   * This utility method is intended to be used for testing only.
   * For production, it's recommended to run any DDL statements before the system is started.
   *
   * It will use the database settings found under `jdbc-journal`, or `pekko-persistence-jdbc.shared-databases` if configured so.
   * See https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#sharing-the-database-connection-pool-between-the-journals for details.
   *
   * @param script the DDL script. The passed script can contain more then one SQL statements separated by a ; (semi-colon).
   */
  @ApiMayChange
  def applyScript(script: String)(implicit actorSystem: ClassicActorSystemProvider): Future[Done] =
    applyScript(script, separator = ";", configKey = "jdbc-journal")

  /**
   * This method can be used to load alternative DDL scripts.
   *
   * This utility method is intended to be used for testing only.
   * For production, it's recommended to create the table with DDL statements before the system is started.
   *
   * It will use the database settings found under `configKey`, or `pekko-persistence-jdbc.shared-databases` if configured so.
   * See https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#sharing-the-database-connection-pool-between-the-journals for details.
   *
   * @param script the DDL script. The passed `script` can contain more then one SQL statements.
   * @param separator used to separate the different DDL statements.
   * @param configKey the database configuration key to use. Can be `jdbc-journal` or `jdbc-snapshot-store`.
   */
  @ApiMayChange
  def applyScript(script: String, separator: String, configKey: String)(
      implicit actorSystem: ClassicActorSystemProvider): Future[Done] =
    SchemaUtilsImpl.applyScript(script, separator, configKey, logger)

}
