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

package org.apache.pekko.persistence.jdbc.testkit.internal

import java.sql.Statement
import scala.concurrent.Future
import org.apache.pekko
import pekko.Done
import pekko.actor.ClassicActorSystemProvider
import pekko.annotation.InternalApi
import pekko.dispatch.Dispatchers
import pekko.persistence.jdbc.db.{ MariaDBProfile, SlickDatabase, SlickExtension }
import com.typesafe.config.Config
import org.slf4j.Logger
import slick.jdbc.H2Profile
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile
import slick.jdbc.OracleProfile
import slick.jdbc.PostgresProfile
import slick.jdbc.SQLServerProfile

/**
 * INTERNAL API
 */
@InternalApi
private[jdbc] object SchemaUtilsImpl {

  def legacy(configKey: String, config: Config): Boolean =
    config.getConfig(configKey).getString("dao") != "org.apache.pekko.persistence.jdbc.journal.dao.DefaultJournalDao"

  /**
   * INTERNAL API
   */
  @InternalApi
  private[jdbc] def dropIfExists(configKey: String, logger: Logger)(
      implicit actorSystem: ClassicActorSystemProvider): Future[Done] = {
    val slickDb: SlickDatabase = loadSlickDatabase(configKey)
    val (fileToLoad, separator) =
      dropScriptFor(
        slickProfileToSchemaType(slickDb.profile),
        legacy(configKey, actorSystem.classicSystem.settings.config))

    val blockingEC = actorSystem.classicSystem.dispatchers.lookup(Dispatchers.DefaultBlockingDispatcherId)
    Future(applyScriptWithSlick(fromClasspathAsString(fileToLoad), separator, logger, slickDb.database))(blockingEC)
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  private[jdbc] def createIfNotExists(configKey: String, logger: Logger)(
      implicit actorSystem: ClassicActorSystemProvider): Future[Done] = {

    val slickDb: SlickDatabase = loadSlickDatabase(configKey)
    val (fileToLoad, separator) =
      createScriptFor(
        slickProfileToSchemaType(slickDb.profile),
        legacy(configKey, actorSystem.classicSystem.settings.config))

    val blockingEC = actorSystem.classicSystem.dispatchers.lookup(Dispatchers.DefaultBlockingDispatcherId)
    Future(applyScriptWithSlick(fromClasspathAsString(fileToLoad), separator, logger, slickDb.database))(blockingEC)
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  private[jdbc] def applyScript(script: String, separator: String, configKey: String, logger: Logger)(
      implicit actorSystem: ClassicActorSystemProvider): Future[Done] = {

    val blockingEC = actorSystem.classicSystem.dispatchers.lookup(Dispatchers.DefaultBlockingDispatcherId)
    Future(applyScriptWithSlick(script, separator, logger, loadSlickDatabase(configKey).database))(blockingEC)
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  private[jdbc] def dropWithSlick(schemaType: SchemaType, logger: Logger, db: Database, legacy: Boolean): Done = {
    val (fileToLoad, separator) = dropScriptFor(schemaType, legacy)
    SchemaUtilsImpl.applyScriptWithSlick(SchemaUtilsImpl.fromClasspathAsString(fileToLoad), separator, logger, db)
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  private[jdbc] def dropWithSlickButChangeSchema(schemaType: SchemaType, logger: Logger, db: Database,
      oldSchemaName: String, newSchemaName: String): Done = {
    val (fileToLoad, separator) = dropScriptFor(schemaType, false)
    val script = SchemaUtilsImpl.fromClasspathAsString(fileToLoad)
      .replaceAll(s"$oldSchemaName.", s"$newSchemaName.")
    SchemaUtilsImpl.applyScriptWithSlick(script, separator, logger, db)
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  private[jdbc] def createWithSlick(schemaType: SchemaType, logger: Logger, db: Database, legacy: Boolean): Done = {
    val (fileToLoad, separator) = createScriptFor(schemaType, legacy)
    SchemaUtilsImpl.applyScriptWithSlick(SchemaUtilsImpl.fromClasspathAsString(fileToLoad), separator, logger, db)
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  private[jdbc] def createWithSlickButChangeSchema(schemaType: SchemaType, logger: Logger, db: Database,
      oldSchemaName: String, newSchemaName: String): Done = {
    val (fileToLoad, separator) = createScriptFor(schemaType, false)
    val script = SchemaUtilsImpl.fromClasspathAsString(fileToLoad)
      .replaceAll(s"$oldSchemaName.", s"$newSchemaName.")
    val scriptWithSchemaCreate = s"CREATE SCHEMA IF NOT EXISTS $newSchemaName$separator$script"
    SchemaUtilsImpl.applyScriptWithSlick(scriptWithSchemaCreate, separator, logger, db)
  }

  private def applyScriptWithSlick(script: String, separator: String, logger: Logger, database: Database): Done = {

    def withStatement(f: Statement => Unit): Done = {
      val session = database.createSession()
      try session.withStatement()(f)
      finally session.close()
      Done
    }

    withStatement { stmt =>
      val lines = script.split(separator).map(_.trim)
      for {
        line <- lines if line.nonEmpty
      } yield {
        logger.debug(s"applying DDL: $line")

        try stmt.executeUpdate(line)
        catch {
          case t: java.sql.SQLException =>
            logger.debug(s"Exception while applying SQL script", t)
        }
      }
    }
  }

  private def dropScriptFor(schemaType: SchemaType, legacy: Boolean): (String, String) = {
    val suffix = if (legacy) "-legacy" else ""
    schemaType match {
      case Postgres          => (s"schema/postgres/postgres-drop-schema$suffix.sql", ";")
      case MySQL             => (s"schema/mysql/mysql-drop-schema$suffix.sql", ";")
      case MariaDB if legacy => throw new IllegalArgumentException(s"Invalid legacy schema request for $schemaType")
      case MariaDB           => (s"schema/mariadb/mariadb-drop-schema$suffix.sql", ";")
      case Oracle            => (s"schema/oracle/oracle-drop-schema$suffix.sql", "/")
      case SqlServer         => (s"schema/sqlserver/sqlserver-drop-schema$suffix.sql", ";")
      case H2                => (s"schema/h2/h2-drop-schema$suffix.sql", ";")
      case _                 => throw new UnsupportedOperationException(s"Unsupported schema request for $schemaType")
    }
  }

  private def createScriptFor(schemaType: SchemaType, legacy: Boolean): (String, String) = {
    val suffix = if (legacy) "-legacy" else ""
    schemaType match {
      case Postgres          => (s"schema/postgres/postgres-create-schema$suffix.sql", ";")
      case MySQL             => (s"schema/mysql/mysql-create-schema$suffix.sql", ";")
      case MariaDB if legacy => throw new IllegalArgumentException(s"Invalid legacy schema request for $schemaType")
      case MariaDB           => (s"schema/mariadb/mariadb-create-schema$suffix.sql", ";")
      case Oracle            => (s"schema/oracle/oracle-create-schema$suffix.sql", "/")
      case SqlServer         => (s"schema/sqlserver/sqlserver-create-schema$suffix.sql", ";")
      case H2                => (s"schema/h2/h2-create-schema$suffix.sql", ";")
      case _                 => throw new UnsupportedOperationException(s"Unsupported schema request for $schemaType")
    }
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  private[jdbc] def slickProfileToSchemaType(profile: JdbcProfile): SchemaType =
    profile match {
      case PostgresProfile  => Postgres
      case MySQLProfile     => MySQL
      case MariaDBProfile   => MariaDB
      case OracleProfile    => Oracle
      case SQLServerProfile => SqlServer
      case H2Profile        => H2
      case _                => throw new IllegalArgumentException(s"Invalid profile $profile encountered")
    }

  /**
   * INTERNAL API
   */
  @InternalApi
  private[jdbc] def fromClasspathAsString(fileName: String): String = {
    val is = getClass.getClassLoader.getResourceAsStream(fileName)
    io.Source.fromInputStream(is).mkString
  }

  private def loadSlickDatabase(configKey: String)(implicit actorSystem: ClassicActorSystemProvider) = {
    val journalConfig = actorSystem.classicSystem.settings.config.getConfig(configKey)
    SlickExtension(actorSystem).database(journalConfig)
  }

}
