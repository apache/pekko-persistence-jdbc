# Migration from Akka Persistence JDBC to Pekko Persistence JDBC 1.0.x/1.1.x

* If you are looking to migrate from [Akka Persistence JDBC](https://doc.akka.io/docs/akka-persistence-jdbc/current/migration.html), you should upgrade to 5.1.x before attempting to migrate to Pekko's equivalent.
* If you are using a newer version of Akka Persistence JDBC, it might be best to compare your table definitions with the Pekko table definitions of the version of Pekko Persistence JDBC that you intend to migrate to. It is possible that Akka have added changes that are not compatible with Pekko supports.
* The [Pekko Migration Guide](https://pekko.apache.org/docs/pekko/1.0/project/migration-guides.html) is a good summary of the changes that you need to make when switching from Akka to Pekko.

# Migrating to 1.2.x

It is recommended that you read the section about DB Schema Changes in the @ref[1.2.0 Release Notes](release-notes/releases-1.2.md).
