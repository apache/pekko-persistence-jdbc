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

# Release Notes (1.3.x)

## 1.3.0

Release notes for Apache Pekko Persistence JDBC 1.3.0. See [GitHub Milestone](https://github.com/apache/pekko-persistence-jdbc/milestone/9?closed=1) for a fuller list of changes.

### Bug Fix

* Fix incorrect implementation of Durable State deleteObject(id, revision) ([PR505](https://github.com/apache/pekko-persistence-jdbc/pull/505))
* Emit all messages across sequence number gaps in messagesWithBatch) ([PR517](https://github.com/apache/pekko-persistence-jdbc/pull/517))
