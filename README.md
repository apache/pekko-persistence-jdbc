# Apache Pekko Persistence JDBC
## Use JDBC-compatible databases with Pekko Persistence

[![License](https://img.shields.io/:license-Apache%202-red.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)

pekko-persistence-jdbc writes journal and snapshot entries to a configured JDBC store. It implements the full pekko-persistence-query API and is therefore very useful for implementing DDD-style 
application models using Pekko for creating reactive applications.

Please note that the H2 database is not recommended to be used as a production database, and support for H2 is primarily for testing purposes.

## Documentation

* [current Apache Pekko Persistence JDBC documentation](https://pekko.apache.org/docs/pekko-persistence-jdbc/current/)

## Release notes

The release notes can be found [here](https://github.com/apache/incubator-pekko-persistence-jdbc/releases).

## Community

There are several ways to interact with the Pekko community:

- [GitHub discussions](https://github.com/apache/incubator-pekko-persistence-jdbc/discussions): for questions and general discussion.
- [Pekko dev mailing list](https://lists.apache.org/list.html?dev@pekko.apache.org): for Pekko development discussions.
- [Pekko users mailing list](https://lists.apache.org/list.html?users@pekko.apache.org): for Pekko user discussions.
- [GitHub issues](https://github.com/apache/incubator-pekko-persistence-jdbc/issues): for bug reports and feature requests. Please search the existing issues before creating new ones. If you are unsure whether you have found a bug, consider asking in GitHub discussions or the mailing list first.

## Contributing

Contributions are *very* welcome! The Apache Pekko team appreciates community contributions by both those new to Pekko and those more experienced.

If you find an issue that you'd like to see fixed, the quickest way to make that happen is to implement the fix and submit a pull request.

Refer to the [CONTRIBUTING.md](CONTRIBUTING.md) file for more details about the workflow, and general hints on how to prepare your pull request.

You can also ask for clarifications or guidance in GitHub issues directly.
