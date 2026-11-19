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

# Release Notes (1.0.x)

## 1.0.0
Apache Pekko Persistence JDBC 1.0.0 is based on Akka Persistence JDBC 5.1.0. Pekko came about as a result of Lightbend's decision to make future
Akka releases under a [Business Software License](https://www.lightbend.com/blog/why-we-are-changing-the-license-for-akka),
a license that is not compatible with Open Source usage.

Apache Pekko has changed the package names, among other changes. The new packages begin with `org.apache.pekko.persistence.jdbc` instead of `akka.persistence.jdbc`.

Config names have changed to use `pekko` instead of `akka` in their names.

Users switching from Akka to Pekko should read our [Migration Guide](https://pekko.apache.org/docs/pekko/1.0/project/migration-guides.html).

Generally, we have tried to make it as easy as possible to switch existing Akka based projects over to using Pekko.

We have gone through the code base and have tried to properly acknowledge all third party source code in the
Apache Pekko code base. If anyone believes that there are any instances of third party source code that is not
properly acknowledged, please get in touch.

### Bug Fixes
We haven't had to fix any significant bugs that were in Akka Persistence JDBC 5.1.0.

### Dependency Upgrades
We have tried to limit the changes to third party dependencies that were used in Akka Persistence JDBC 5.1.0. These are some exceptions:

* we updated some of the JDBC jars that we use in testing
    * postgresql and h2 are 2 examples where there have been recent security fixes
