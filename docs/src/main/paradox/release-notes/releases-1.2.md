# Release Notes (1.2.x)

## 1.2.0

Release notes for Apache Pekko Persistence JDBC 1.2.0. See [GitHub Milestone](https://github.com/apache/pekko-persistence-jdbc/milestone/6?closed=1) for a fuller list of changes.

This release includes some changes from Akka Persistence JDBC 5.2.0, which have recently become available under the Apache License, Version 2.0.

### Schema Changes

If you have a pre-existing Pekko Persistence JDBC database and want to upgrade to 1.2.0, it is encouraged for you to update the table definitions. This is not done automatically. We do not expect that there will be issues if you don't update the table definitions. Please take care when updating the table definitions (if you do). We would recommend backing up your database first and ideally, trying the whole process in a test environment before approaching your production database.

We don't provide migration scripts but we believe that it should be straightforward to review your existing table definitions and check them against the changes in these Pull Requests.

* Change Oracle DELETED column type from CHAR to NUMBER ([PR323](https://github.com/apache/pekko-persistence-jdbc/pull/323))
* Add MariaDB schema support. Pre-existing users can stick with using the MySQL dialect that we already supported. The explicit MariaDB support allows use of Durable State persistence that works well in MariaDB but that relies on a feature not availabe in MySQL ([PR367](https://github.com/apache/pekko-persistence-jdbc/pull/367))
* Implement Durable State handling with MySQL. MariaDB users should prefer the specific MariaDB solution in PR367. ([PR378](https://github.com/apache/pekko-persistence-jdbc/pull/378))
* use NVARCHAR instead of VARCHAR for SQL Server ([PR382](https://github.com/apache/pekko-persistence-jdbc/pull/382))

### Changes

* Limit events by tag query ordering sizes ([PR381](https://github.com/apache/pekko-persistence-jdbc/pull/381))
