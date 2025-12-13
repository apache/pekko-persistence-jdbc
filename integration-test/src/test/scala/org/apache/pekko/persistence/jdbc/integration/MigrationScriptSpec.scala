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

package org.apache.pekko.persistence.jdbc.integration

import java.sql.Statement
import com.typesafe.config.{ Config, ConfigFactory }
import org.apache.pekko
import pekko.Done
import pekko.actor.ActorSystem
import pekko.persistence.jdbc.state.scaladsl.StateSpecBase
import pekko.persistence.jdbc.testkit.internal.{ SchemaType, SqlServer }
import slick.jdbc.JdbcBackend.Database

import scala.util.Using

abstract class MigrationScriptSpec(config: Config, schemaType: SchemaType) extends StateSpecBase(config, schemaType) {

  implicit lazy val system: ActorSystem = ActorSystem("migration-test", config)

  protected def withStatement(database: Database)(f: Statement => Unit): Done = {
    val session = database.createSession()
    try session.withStatement()(f)
    finally session.close()
    Done
  }

  protected def applyScriptWithSlick(script: String, database: Database): Done = {
    withStatement(database) { stmt =>
      stmt.executeUpdate(script)
    }
  }

  protected def applyScriptWithSlick(script: String, separator: String, database: Database): Done = {
    withStatement(database) { stmt =>
      val lines = script.split(separator).map(_.trim)
      for {
        line <- lines if line.nonEmpty
      } yield {
        stmt.executeUpdate(line)
      }
    }
  }
}

class SqlServerMigrationScriptSpec extends MigrationScriptSpec(
      ConfigFactory.load("sqlserver-application.conf"),
      SqlServer
    ) {
  "SQL Server nvarchar migration script" must {
    "apply without errors" in {
      val scriptPath = getClass.getResource("/schema/sqlserver/migration-1.3.0/sqlserver-nvarchar-migration.sql").getPath
      val sql = Using(scala.io.Source.fromFile(scriptPath))(_.mkString).get
      val parts = sql.split("(?<=END;)")

      parts.length should be > 1

      applyScriptWithSlick(parts.head, db)
      parts.tail.foreach(part => applyScriptWithSlick(part, db))
    }
  }
}

class MariaDBMigrationScriptSpec extends MigrationScriptSpec(
      ConfigFactory.load("mariadb-application.conf"),
      SqlServer
    ) {
  "MariaDB migration script" must {
    "apply the schema and the migration without errors" in {
      val schemaPath = getClass.getResource("/schema/mariadb/mariadb-create-schema.sql").getPath
      val schema = Using(scala.io.Source.fromFile(schemaPath))(_.mkString).get
      applyScriptWithSlick(schema, db)

      val migrationPath = getClass.getResource("/schema/mariadb/migration-1.3.0/mariadb-durable-state-migration.sql").getPath
      val migration = Using(scala.io.Source.fromFile(migrationPath))(_.mkString).get
      applyScriptWithSlick(migration, db)
    }
  }
}

class MySQLMigrationScriptSpec extends MigrationScriptSpec(
      ConfigFactory.load("mysql-application.conf"),
      SqlServer
    ) {
  "MySQL migration script" must {
    "apply the schema and the migration without errors" in {
      val schemaPath = getClass.getResource("/schema/mysql/mysql-create-schema.sql").getPath
      val schema = Using(scala.io.Source.fromFile(schemaPath))(_.mkString).get

      // Each statement executed as standalone
      schema.split(";")
        .map(_.trim)
        .filter(_.nonEmpty)
        .foreach(statement => applyScriptWithSlick(statement, db))

      val migrationPath = getClass.getResource("/schema/mysql/migration-1.3.0/mysql-durable-state-migration.sql").getPath
      val migration = Using(scala.io.Source.fromFile(migrationPath))(_.mkString).get

      migration.split(";")
        .map(_.trim)
        .filter(_.nonEmpty)
        .foreach(statement => applyScriptWithSlick(statement, db))
    }
  }
}
