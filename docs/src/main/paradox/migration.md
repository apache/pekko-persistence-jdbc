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

# Migration

* If you are looking to migrate from [Akka Persistence JDBC](https://doc.akka.io/docs/akka-persistence-jdbc/current/migration.html), you should upgrade to 5.1.x before attempting to migrate to Pekko's equivalent.
* If you are using a newer version of Akka Persistence JDBC, it might be best to compare your table definitions with the Pekko table definitions of the version of Pekko Persistence JDBC that you intend to migrate to. It is possible that Akka have added changes that are not compatible with what Pekko supports.
* The [Pekko Migration Guide](https://pekko.apache.org/docs/pekko/1.0/project/migration-guides.html) is a good summary of the changes that you need to make when switching from Akka to Pekko.

## Migrating to 1.2.x

It is recommended that you read the section about DB Schema Changes in the @ref[1.2.0 Release Notes](release-notes/releases-1.2.md).

## Migrating to version 2.0.0

Release `2.0.0` is a new major version. Upgrade to the latest 1.x release first, then work through this checklist.
The @ref[2.0.0 Release Notes](release-notes/releases-2.0.md) list all the changes.

1. **Platform**: Java 17 or later, Scala 2.13 or Scala 3, and Pekko 2.0 are required. Scala 2.12 is no longer supported.
   Follow the [Pekko 2.0 migration guide](https://pekko.apache.org/docs/pekko/current/project/migration-guides.html)
   for the changes in Pekko itself.
2. **Legacy schema**: if you still have data in the pre-5.0.0 `journal` and `snapshot` tables, migrate it with the
   1.x migrator *before* upgrading. See [Legacy schema support removed](#legacy-schema-support-removed).
3. **Removed APIs**: code that was deprecated in 1.x has been removed, as has the `PluginVersionChecker`. The
   constructor of the javadsl `JdbcDurableStateStore` is no longer public; obtain the store via
   `DurableStateStoreRegistry`. Fix any deprecation warnings while still on 1.x to make this step easier.
4. **Schema changes**: the `migration-2.0.0` scripts are optional for existing databases, but recommended. See
   [`event_tag` schema change](#event-tag-schema-change) and [Other schema changes](#other-schema-changes).
   Back up your database and test the scripts in a test environment before applying them in production.

### Legacy schema support removed

Release `2.0.0` removes support for the legacy `journal` and `snapshot` tables that were used by
akka-persistence-jdbc before version 5.0.0. The `ByteArrayJournalDao`, `ByteArrayReadJournalDao` and
`ByteArraySnapshotDao` classes, the `legacy_journal` and `legacy_snapshot` table configuration sections,
the `tagSeparator` settings and the `*-legacy.sql` schema scripts are gone. The migrator module has also been
removed; it is available in 1.4.0 and later 1.x releases.

If you still store data in the legacy tables, migrate it to the current schema with the
`pekko-persistence-jdbc-migrator` module from version 1.4.0 or a later 1.x release before upgrading to `2.0.0`.
You can depend on the published artifact, or build the module from source. Apache source releases are available
from the [Pekko download page](https://pekko.apache.org/download.html#pekko-persistence-jdbc), along with their
signatures and checksums; releases that are no longer listed there are kept in the
[Apache archive](https://archive.apache.org/dist/pekko/). The source is also in git, for example on the
[1.4.x branch](https://github.com/apache/pekko-persistence-jdbc/tree/1.4.x).

The migrator is a library with no main class: you call `JournalMigrator` and `SnapshotMigrator` from a small
program of your own. The
[1.4.x migration guide](https://pekko.apache.org/docs/pekko-persistence-jdbc/1.4/migration.html#migrating-from-the-legacy-schema)
describes the steps and includes an example.

### `event_tag` schema change

Release `2.0.0` changes the schema of the `event_tag` table.

The previous version was using an auto-increment column as a primary key and foreign key on the `event_tag` table. As a result, the insert of multiple events in batch was not performant.

While in `2.0.0`, the primary key and foreign key on the `event_tag` table have been replaced with a primary key from the `event_journal` table. In order to migrate to the new schema, we made a **migration script** (see the `migration-2.0.0` directory for your database type in our [git repo](https://github.com/apache/pekko-persistence-jdbc/tree/main/core/src/main/resources/schema) or the source release) which is capable of creating the new column, migrating the rows and adding the new constraints.

By default, the plugin will behave as in previous version. If you want to use the new `event_tag` keys, you need to run a multiple-phase rollout:

1. apply the first part of the migration script and then redeploy your application with the default settings.
2. apply the second part of the migration script that will migrate the rows and adapt the constraints.
3. redeploy the application by disabling the legacy-mode:

```config
jdbc-journal {
  tables {
    // ...
    event_tag {
      // ...
      // enable the new tag key
      legacy-tag-key = false
    }
  }
}
// or simply configure via flattened style
jdbc-journal.tables.event_tag.legacy-tag-key = false
```

### Other schema changes

The `migration-2.0.0` directories also contain these scripts. They are not applied automatically.

* `*-event-tag-index-migration.sql` (Oracle, PostgreSQL and SQL Server): adds an index on the `tag` column of the
  `event_tag` table. The primary key of `event_tag` does not start with `tag`, so it cannot be used efficiently by
  tag queries such as `eventsByTag`. New databases created from the 2.0.0 schema scripts already have this index.
* `oracle-durable-state-tag-nullable-migration.sql` (Oracle): makes the `TAG` column of the `DURABLE_STATE` table
  nullable, so that Oracle behaves like the other databases when durable state is stored without a tag.
