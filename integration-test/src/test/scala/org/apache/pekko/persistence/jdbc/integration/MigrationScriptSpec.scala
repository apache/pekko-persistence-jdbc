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
import pekko.persistence.jdbc.testkit.internal.{ Oracle, SchemaType, SqlServer }
import slick.jdbc.JdbcBackend.Database

abstract class MigrationScriptSpec(config: Config, schemaType: SchemaType) extends StateSpecBase(config, schemaType) {

  implicit lazy val system: ActorSystem = ActorSystem("migration-test", config)

  protected def applyScriptWithSlick(script: String, separator: String, database: Database): Done = {

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
        stmt.executeUpdate(line)
      }
    }
  }
}

class OracleMigrationScriptSpec extends MigrationScriptSpec(
      ConfigFactory.load("oracle-shared-db-application.conf"), Oracle) {
  "Oracle migration script" must {
    "apply without errors" in {
      val script = getClass.getResource("/schema/sqlserver/oracle-number-boolean-migration.sql").getPath
      val sql = scala.io.Source.fromFile(script).mkString
      applyScriptWithSlick(sql, "/", db)
    }
  }
}

class SqlServerMigrationScriptSpec extends MigrationScriptSpec(
      ConfigFactory.load("sqlserver-shared-db-application.conf"), SqlServer) {
  "SQL Server nvarchar migration script" must {
    "apply without errors" in {
      val script = getClass.getResource("/schema/sqlserver/sqlserver-nvarchar-migration.sql").getPath
      val sql = scala.io.Source.fromFile(script).mkString
      applyScriptWithSlick(sql, ";", db)
    }
  }
}
