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

package org.apache.pekko.persistence.jdbc.util

import java.sql.Statement

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.persistence.jdbc.testkit.internal.SchemaType
import pekko.persistence.jdbc.testkit.internal.SchemaUtilsImpl
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcBackend.Session

/**
 * INTERNAL API
 */
@InternalApi
private[jdbc] trait DropCreate {

  private val logger = LoggerFactory.getLogger(this.getClass)
  def db: Database
  def config: Config

  def newDao: Boolean = !SchemaUtilsImpl.legacy("jdbc-journal", config)

  /**
   * INTERNAL API
   */
  @InternalApi
  private[jdbc] def dropAndCreate(schemaType: SchemaType): Unit = {
    // blocking calls, usually done in our before test methods
    SchemaUtilsImpl.dropWithSlick(schemaType, logger, db, !newDao)
    SchemaUtilsImpl.createWithSlick(schemaType, logger, db, !newDao)
  }

  def withSession[A](f: Session => A): A = {
    withDatabase { db =>
      val session = db.createSession()
      try f(session)
      finally session.close()
    }
  }

  def withStatement[A](f: Statement => A): A =
    withSession(session => session.withStatement()(f))

  /**
   * INTERNAL API
   */
  @InternalApi
  private[jdbc] def withDatabase[A](f: Database => A): A =
    f(db)
}
