# Migration from Akka Persistence JDBC to Pekko Persistence JDBC 1.0.x/1.1.x

* If you are looking to migrate from [Akka Persistence JDBC](https://doc.akka.io/docs/akka-persistence-jdbc/current/migration.html), you should upgrade to 5.1.x before attempting to migrate to Pekko's equivalent.
* If you are using a newer version of Akka Persistence JDBC, it might be best to compare your table definitions with the Pekko table definitions of the version of Pekko Persistence JDBC that you intend to migrate to. It is possible that Akka have added changes that are not compatible with Pekko supports.
* The [Pekko Migration Guide](https://pekko.apache.org/docs/pekko/1.0/project/migration-guides.html) is a good summary of the changes that you need to make when switching from Akka to Pekko.

# Migrating to 1.2.x

It is recommended that you read the section about DB Schema Changes in the @ref[1.2.0 Release Notes](release-notes/releases-1.2.md).

# Migrating from the legacy schema

Data written by Akka Persistence JDBC before version 5.0.0 lives in the legacy `journal` and `snapshot` tables.
Pekko Persistence JDBC 1.x can still read and write those tables through the legacy DAOs, but support for the legacy
schema is removed in 2.0.0. The `pekko-persistence-jdbc-migrator` module copies the legacy rows into the current
`event_journal`, `event_tag` and `snapshot` tables.

@@dependency [sbt,Maven,Gradle] {
  group=org.apache.pekko
  artifact=pekko-persistence-jdbc-migrator_$scala.binary.version$
  version=$project.version$
}

The JDBC driver for your database is not a transitive dependency of the migrator; add it yourself.

## Running the migration

1. Create the new tables with the schema script for your database (see @ref[Configuration](configuration.md)) and keep
   the legacy tables in place.
2. The legacy snapshot table and the new snapshot table both default to the name `snapshot`. If both live in the same
   schema, give the legacy table a distinct name in the configuration:

    ```
    jdbc-snapshot-store.tables.legacy_snapshot.tableName = "legacy_snapshot"
    ```

    The legacy journal table defaults to `journal` and needs no change.
3. Stop the applications that write to the legacy tables.
4. Run the migrators from a small program that loads the same `jdbc-journal`, `jdbc-read-journal` and
   `jdbc-snapshot-store` configuration as your application:

    ```scala
    import org.apache.pekko.actor.ActorSystem
    import org.apache.pekko.persistence.jdbc.migrator.{ JournalMigrator, SnapshotMigrator }

    import scala.concurrent.Await
    import scala.concurrent.duration.Duration

    implicit val system: ActorSystem = ActorSystem("migration")
    val profile = slick.jdbc.PostgresProfile

    Await.result(JournalMigrator(profile).migrate(), Duration.Inf)
    Await.result(SnapshotMigrator(profile).migrateAll(), Duration.Inf)
    Await.result(system.terminate(), Duration.Inf)
    ```

    `SnapshotMigrator.migrateLatest()` migrates only the most recent snapshot of each persistence id instead of all of
    them.
5. Configure the applications to use the default DAOs (the default configuration) and start them again.
