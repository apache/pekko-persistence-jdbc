# Release Notes (2.0.x)

## 2.0.0-M1

Release notes for Apache Pekko Persistence JDBC 2.0.0-M1. See [GitHub Milestone](https://github.com/apache/pekko-persistence-jdbc/milestone/7?closed=1) for a fuller list of changes.

This is a milestone release and is aimed at testing this new major version
by early adopters. This is experimental. This release should not be used in production.

### Main changes

* Pekko 2.0.0-M1 is the new minimum Pekko version
* Java 17 is the new minimum JRE version
* Scala 2.12 support dropped
* A lot of deprecated code removed
* Slick 3.6.1

## 2.0.0-M2

### Main changes

* Support for the legacy pre-5.0.0 akka-persistence-jdbc schema has been removed, along with the unpublished migrator module. See @ref[Migration](../migration.md#legacy-schema-support-removed).
