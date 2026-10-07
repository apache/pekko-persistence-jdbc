/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.persistence.jdbc.util

import java.util.concurrent.CopyOnWriteArrayList

import scala.annotation.unused
import scala.jdk.CollectionConverters._

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.annotation.InternalApi
import pekko.persistence.jdbc.config.SlickConfiguration
import pekko.persistence.jdbc.db.{ SlickDatabase, SlickDatabaseProvider }
import com.typesafe.config.Config
import slick.jdbc.JdbcBackend.Database

/**
 * INTERNAL API
 *
 * A [[SlickDatabaseProvider]] that behaves like the default provider, but keeps track of every database it hands out
 * so that tests can assert on the lifecycle of those databases.
 */
@InternalApi
private[jdbc] class RecordingSlickDatabaseProvider(@unused system: ActorSystem) extends SlickDatabaseProvider {
  override def database(config: Config): SlickDatabase = {
    val slickDatabase =
      SlickDatabase.initializeEagerly(config, new SlickConfiguration(config.getConfig("slick")), "slick")
    RecordingSlickDatabaseProvider.record(slickDatabase.database)
    slickDatabase
  }
}

/**
 * INTERNAL API
 */
@InternalApi
private[jdbc] object RecordingSlickDatabaseProvider {
  private val created = new CopyOnWriteArrayList[Database]

  def record(database: Database): Unit = created.add(database)

  def clear(): Unit = created.clear()

  def databases: Seq[Database] = created.asScala.toList
}
