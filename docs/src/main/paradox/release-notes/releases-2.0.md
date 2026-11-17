<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements. See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License. You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Release Notes (2.0.x)

## 2.0.0

Release notes for Apache Pekko Persistence JDBC 2.0.0. See the GitHub milestones for
[2.0.0-M1](https://github.com/apache/pekko-persistence-jdbc/milestone/7?closed=1) and
[2.0.0-M2](https://github.com/apache/pekko-persistence-jdbc/milestone/8?closed=1) for a fuller list of changes.

This is a new major release. Please read the @ref[Migration Guide](../migration.md#migrating-to-version-2-0-0)
before upgrading from a 1.x release.

### Main changes

* Pekko 2.0 is the new minimum Pekko version
* Java 17 is the new minimum JRE version
* Scala 2.12 support dropped; Scala 2.13 and Scala 3 are supported
* Slick 3.6.1
* A lot of deprecated code removed ([PR335](https://github.com/apache/pekko-persistence-jdbc/pull/335), [PR345](https://github.com/apache/pekko-persistence-jdbc/pull/345))
* Use the Pekko `CurrentLastSequenceNumberByPersistenceIdQuery` trait ([PR272](https://github.com/apache/pekko-persistence-jdbc/pull/272))
* The internals of the javadsl `JdbcDurableStateStore` are no longer public. Obtain the store via `DurableStateStoreRegistry` ([PR661](https://github.com/apache/pekko-persistence-jdbc/pull/661))
* The Oracle JDBC driver used for testing is now `ojdbc11` instead of `ojdbc8` ([PR612](https://github.com/apache/pekko-persistence-jdbc/pull/612))

### Legacy schema support removed

Support for the legacy pre-5.0.0 akka-persistence-jdbc schema (the `journal` and `snapshot` tables) has been removed,
along with the migrator module ([PR621](https://github.com/apache/pekko-persistence-jdbc/pull/621)).
If you still have data in the legacy tables, use the migrator from 1.4.0 or a later 1.x release to move it to the
current schema before upgrading to 2.0.0. See @ref[Migration](../migration.md#legacy-schema-support-removed).

### Schema changes

Migration scripts for these changes are in the `migration-2.0.0` directories for each database type, in our
[git repo](https://github.com/apache/pekko-persistence-jdbc/tree/main/core/src/main/resources/schema) or the source release.
They are not applied automatically. See @ref[Migration](../migration.md#migrating-to-version-2-0-0).

* Optimized `event_tag` table writes: the `event_tag` table can now use the primary key of the `event_journal` table
  instead of an auto-increment column. This is opt-in via the `legacy-tag-key` setting
  ([PR386](https://github.com/apache/pekko-persistence-jdbc/pull/386), [PR594](https://github.com/apache/pekko-persistence-jdbc/pull/594))
* Add indexes on the tag columns to make tag queries such as `eventsByTag` more efficient ([PR593](https://github.com/apache/pekko-persistence-jdbc/pull/593))
* Make the `TAG` column of the Oracle `DURABLE_STATE` table nullable ([PR485](https://github.com/apache/pekko-persistence-jdbc/pull/485))

### Bug fixes

* Honor snapshot selection lower bounds ([PR618](https://github.com/apache/pekko-persistence-jdbc/pull/618))
* Complete the journal write queue when the plugin actor stops ([PR609](https://github.com/apache/pekko-persistence-jdbc/pull/609))
* Use the configured schema in the H2 durable state sequence lookup ([PR608](https://github.com/apache/pekko-persistence-jdbc/pull/608))
* Close the blob streams that snapshot payloads are read from ([PR607](https://github.com/apache/pekko-persistence-jdbc/pull/607))
* Close Slick databases that are created but never closed ([PR606](https://github.com/apache/pekko-persistence-jdbc/pull/606))

### Performance

* Reduce memory use when replaying messages ([PR648](https://github.com/apache/pekko-persistence-jdbc/pull/648))
* Avoid an event serialization roundtrip for `SerializedEvent` ([PR673](https://github.com/apache/pekko-persistence-jdbc/pull/673))
* Defer debug log message construction to the logging backend ([PR616](https://github.com/apache/pekko-persistence-jdbc/pull/616))

### Changes also in 1.x releases

The following changes were included in 2.0.0 milestones and have also been released in 1.x releases:

* Implement MariaDB DBMS ([PR365](https://github.com/apache/pekko-persistence-jdbc/pull/365))
* Limit events by tag query ordering sizes ([PR380](https://github.com/apache/pekko-persistence-jdbc/pull/380))
* Use NVARCHAR instead of VARCHAR for SQL Server ([PR382](https://github.com/apache/pekko-persistence-jdbc/pull/382))
* Emit all messages across sequence number gaps in messagesWithBatch ([PR517](https://github.com/apache/pekko-persistence-jdbc/pull/517))
* Remove the `PluginVersionChecker` ([PR559](https://github.com/apache/pekko-persistence-jdbc/pull/559))
