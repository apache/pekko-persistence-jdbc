# Release Notes (1.4.x)

## 1.4.0

Release notes for Apache Pekko Persistence JDBC 1.4.0. See [GitHub Milestone](https://github.com/apache/pekko-persistence-jdbc/milestone/10?closed=1) for a fuller list of changes.

### Additions

* New `pekko-persistence-jdbc-migrator` module that copies data from the legacy `journal` and `snapshot` tables to the current `event_journal`, `event_tag` and `snapshot` tables. Support for the legacy schema is removed in 2.0.0, so users of the legacy schema should migrate their data with this module before upgrading. See [Migrating from the legacy schema](https://pekko.apache.org/docs/pekko-persistence-jdbc/1.4/migration.html#migrating-from-the-legacy-schema) ([PR624](https://github.com/apache/pekko-persistence-jdbc/pull/624))

### Changes

* Remove the check for the old `com.github.dnvriend` version of the plugin on the classpath, which is not needed in Pekko ([PR623](https://github.com/apache/pekko-persistence-jdbc/pull/623))
