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

* Support for the legacy pre-5.0.0 akka-persistence-jdbc schema has been removed, along with the migrator module, which is available in 1.4.0 and later 1.x releases. See @ref[Migration](../migration.md#legacy-schema-support-removed).
