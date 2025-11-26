# Migration

* If you are looking to migrate from [Akka Persistence JDBC](https://doc.akka.io/docs/akka-persistence-jdbc/current/migration.html), you should upgrade to v5.1.x before attempting to migrate to Pekko's equivalent.
* The [Pekko Migration Guide](https://pekko.apache.org/docs/pekko/1.0/project/migration-guides.html) is a good summary of the changes that you need to make when switching from Akka to Pekko.

## Migrating to version 2.0.0

Release `2.0.0` change the schema of `event_tag` table.

The previous version was using an auto-increment column as a primary key and foreign key on the `event_tag` table. As a result, the insert of multiple events in batch was not performant.

While in `2.0.0`, the primary key and foreign key on the `event_tag` table have been replaced with a primary key from the `event_journal` table. In order to migrate to the new schema, we made a [**migration script**](https://github.com/apache/pekko-persistence-jdbc/tree/master/core/src/main/resources/schema) which is capable of creating the new column, migrate the rows and add the new constraints.

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
// or simply configue via flatting style
jdbc-journal.tables.event_tag.legacy-tag-key = false
```
