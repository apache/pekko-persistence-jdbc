/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.pekko.persistence.jdbc.integration

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.jdbc.state.scaladsl.DurableStateSequenceActorTest
import org.apache.pekko.persistence.jdbc.testkit.internal.{Oracle, Postgres, SqlServer}

class OracleDurableStateSequenceActorTest
    extends DurableStateSequenceActorTest(ConfigFactory.load("oracle-application.conf"), Oracle) {
  implicit lazy val system: ActorSystem =
    ActorSystem("test", config.withFallback(customSerializers))
}

class SqlServerDurableStateSequenceActorTest
    extends DurableStateSequenceActorTest(ConfigFactory.load("sqlserver-application.conf"), SqlServer) {
  implicit lazy val system: ActorSystem =
    ActorSystem("test", config.withFallback(customSerializers))
}

class PostgresDurableStateSequenceActorTest
    extends DurableStateSequenceActorTest(ConfigFactory.load("postgres-application.conf"), Postgres) {
  implicit lazy val system: ActorSystem =
    ActorSystem("test", config.withFallback(customSerializers))
}
