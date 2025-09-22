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

package org.apache.pekko.persistence.jdbc.testkit.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.Done
import pekko.actor.ClassicActorSystemProvider
import pekko.annotation.ApiMayChange
import pekko.persistence.jdbc.testkit.internal.SchemaUtilsImpl
import org.slf4j.LoggerFactory

import scala.jdk.FutureConverters._

object SchemaUtils {

  private val logger = LoggerFactory.getLogger("org.apache.pekko.persistence.jdbc.testkit.javadsl.SchemaUtils")

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
  def dropIfExists(actorSystem: ClassicActorSystemProvider): CompletionStage[Done] =
    dropIfExists(configKey = "jdbc-journal", actorSystem)

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
  def dropIfExists(configKey: String, actorSystem: ClassicActorSystemProvider): CompletionStage[Done] =
    SchemaUtilsImpl.dropIfExists(configKey, logger)(actorSystem).asJava

  /**
   * Creates the schema for both the journal and the snapshot table using the default schema definition.
   *
   * For information about the different schemas and supported databases consult
   * https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#database-schema
   *
   * This utility method is intended to be used for testing only.
   * For production, it's recommended to create run DDL statements before the system is started.
   *
   * This method will automatically detects the configured database using the settings from `jdbc-journal` config.
   * If configured with `use-shared-db`, it will use the `pekko-persistence-jdbc.shared-databases` definition instead.
   * See https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#sharing-the-database-connection-pool-between-the-journals for details.
   */
  @ApiMayChange
  def createIfNotExists(actorSystem: ClassicActorSystemProvider): CompletionStage[Done] =
    createIfNotExists("jdbc-journal", actorSystem)

  /**
   * Creates the schema for both the journal and the snapshot table using the default schema definition.
   *
   * For information about the different schemas and supported databases consult
   * https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#database-schema
   *
   * This utility method is intended to be used for testing only.
   * For production, it's recommended to create run DDL statements before the system is started.
   *
   * This method will automatically detects the configured database using the settings from `configKey` config.
   * If configured with `use-shared-db`, it will use the `pekko-persistence-jdbc.shared-databases` definition instead.
   * See https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#sharing-the-database-connection-pool-between-the-journals for details.
   *
   * @param configKey the database journal configuration key to use.
   */
  @ApiMayChange
  def createIfNotExists(configKey: String, actorSystem: ClassicActorSystemProvider): CompletionStage[Done] =
    SchemaUtilsImpl.createIfNotExists(configKey, logger)(actorSystem).asJava

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
  def applyScript(script: String, actorSystem: ClassicActorSystemProvider): CompletionStage[Done] =
    applyScript(script, separator = ";", configKey = "jdbc-journal", actorSystem)

  /**
   * This method can be used to load alternative DDL scripts.
   *
   * This utility method is intended to be used for testing only.
   * For production, it's recommended to run any DDL statements before the system is started.
   *
   * It will use the database settings found under `configKey`, or `pekko-persistence-jdbc.shared-databases` if configured so.
   * See https://pekko.apache.org/docs/pekko-persistence-jdbc/current/index.html#sharing-the-database-connection-pool-between-the-journals for details.
   *
   * @param script the DDL script. The passed `script` can contain more then one SQL statements.
   * @param separator used to separate the different DDL statements.
   * @param configKey the database configuration key to use. Can be `jdbc-journal` or `jdbc-snapshot-store`.
   */
  @ApiMayChange
  def applyScript(
      script: String,
      separator: String,
      configKey: String,
      actorSystem: ClassicActorSystemProvider): CompletionStage[Done] =
    SchemaUtilsImpl.applyScript(script, separator, configKey, logger)(actorSystem).asJava
}
