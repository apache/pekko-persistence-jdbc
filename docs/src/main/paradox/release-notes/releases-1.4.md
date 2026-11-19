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

# Release Notes (1.4.x)

## 1.4.0

Release notes for Apache Pekko Persistence JDBC 1.4.0. See [GitHub Milestone](https://github.com/apache/pekko-persistence-jdbc/milestone/10?closed=1) for a fuller list of changes.

### Additions

* New `pekko-persistence-jdbc-migrator` module that copies data from the legacy `journal` and `snapshot` tables to the current `event_journal`, `event_tag` and `snapshot` tables. Support for the legacy schema is removed in 2.0.0, so users of the legacy schema should migrate their data with this module before upgrading. See [Migrating from the legacy schema](https://pekko.apache.org/docs/pekko-persistence-jdbc/1.4/migration.html#migrating-from-the-legacy-schema) ([PR624](https://github.com/apache/pekko-persistence-jdbc/pull/624))

### Changes

* Remove the check for the old `com.github.dnvriend` version of the plugin on the classpath, which is not needed in Pekko ([PR623](https://github.com/apache/pekko-persistence-jdbc/pull/623))
