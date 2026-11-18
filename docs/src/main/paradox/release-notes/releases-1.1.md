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

# Release Notes (1.1.x)

## 1.1.1

Release notes for Apache Pekko Persistence JDBC 1.1.1. See [GitHub Milestone](https://github.com/apache/pekko-persistence-jdbc/milestone/5?closed=1) for a fuller list of changes.

### Bug Fixes

* Durable State: setting non-default schemaName leads to write errors ([PR276](https://github.com/apache/pekko-persistence-jdbc/pull/276))
* Remove unused param in JdbcDurableStateStore causing memory leak ([PR290](https://github.com/apache/pekko-persistence-jdbc/pull/290))


### Additions

* Add support for querying last known sequence number by persistenceId ([PR267](https://github.com/apache/pekko-persistence-jdbc/pull/267))

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
