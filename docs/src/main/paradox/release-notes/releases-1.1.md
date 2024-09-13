# Release Notes (1.1.x)

## 1.1.0

Release notes for Apache Pekko Persistence JDBC 1.1.0. See [GitHub Milestone](https://github.com/apache/pekko-persistence-jdbc/milestone/1?closed=1) for a fuller list of changes.

### Bug Fixes

* DurableState, don't ignore revision in `deleteObject(persistenceId, revision)` ([PR156](https://github.com/apache/pekko-persistence-jdbc/pull/156))
* use new Source.queue to avoid memory leak ([PR175](https://github.com/apache/pekko-persistence-jdbc/pull/175))
* Issue with `tick` parameters in wrong order ([PR183](https://github.com/apache/pekko-persistence-jdbc/pull/183))

### Additions

* Scala 3 support
* Add Durable State support for Oracle and SQL Server (but not yet MySQL) ([PR158](https://github.com/apache/pekko-persistence-jdbc/pull/158))

### Changes

* improve journal deletion performance ([PR169](https://github.com/apache/pekko-persistence-jdbc/pull/169))
* avoid large offset query via limit windowing ([PR180](https://github.com/apache/pekko-persistence-jdbc/pull/180))

### Dependency Upgrades

* slick 3.5.1

We upgraded the JDBC jars for the various databases that we test with. 

* postgresql 42.7
* mysql 8.4
* sql server 12.6
* oracle 23.4
* h2 2.2
