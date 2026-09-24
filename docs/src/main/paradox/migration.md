# Migration

* If you are looking to migrate from [Akka Persistence JDBC](https://doc.akka.io/docs/akka-persistence-jdbc/current/migration.html), you should upgrade to 5.1.x before attempting to migrate to Pekko's equivalent.
* If you are using a newer version of Akka Persistence JDBC, it might be best to compare your table definitions with the Pekko table definitions of the version of Pekko Persistence JDBC that you intend to migrate to. It is possible that Akka have added changes that are not compatible with what Pekko supports.
* The [Pekko Migration Guide](https://pekko.apache.org/docs/pekko/1.0/project/migration-guides.html) is a good summary of the changes that you need to make when switching from Akka to Pekko.

## Migrating to 1.2.x

It is recommended that you read the section about DB Schema Changes in the @ref[1.2.0 Release Notes](release-notes/releases-1.2.md).

## Migrating to version 2.0.0

### Legacy schema support removed

Release `2.0.0` removes support for the legacy `journal` and `snapshot` tables that were used by
akka-persistence-jdbc before version 5.0.0. The `ByteArrayJournalDao`, `ByteArrayReadJournalDao` and
`ByteArraySnapshotDao` classes, the `legacy_journal` and `legacy_snapshot` table configuration sections,
the `tagSeparator` settings and the `*-legacy.sql` schema scripts are gone. The unpublished migrator
module has also been removed.

If you still store data in the legacy tables, migrate it to the current schema with a
[1.3.x release](https://github.com/apache/pekko-persistence-jdbc/tree/1.3.x) before upgrading to `2.0.0`.
The `migrator` module on the `1.3.x` branch can be run from a source checkout with `sbt "migrator / run"`.

### `event_tag` schema change

Release `2.0.0` changes the schema of the `event_tag` table.

The previous version was using an auto-increment column as a primary key and foreign key on the `event_tag` table. As a result, the insert of multiple events in batch was not performant.

While in `2.0.0`, the primary key and foreign key on the `event_tag` table have been replaced with a primary key from the `event_journal` table. In order to migrate to the new schema, we made a [**migration script**](https://github.com/apache/pekko-persistence-jdbc/tree/master/core/src/main/resources/schema) which is capable of creating the new column, migrating the rows and adding the new constraints.

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
