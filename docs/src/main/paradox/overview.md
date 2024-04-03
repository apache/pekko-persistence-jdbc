# Overview

The Apache Pekko Persistence JDBC plugin allows for using JDBC-compliant databases as backend for @extref:[Apache Pekko Persistence](pekko:persistence.html) and @extref:[Apache Pekko Persistence Query](pekko:persistence-query.html).

This plugin supports both **Event Sourcing** and **Durable State** persistence modes, but does not support DurableState under the backend of MySQL.

In EventSourcing mode, pekko-persistence-jdbc writes journal and snapshot entries to a configured JDBC store. It also implements the full pekko-persistence-query API and is therefore very useful for implementing DDD-style application models using Apache Pekko and Scala for creating reactive applications.

In DurableState mode, pekko-persistence-jdbc writes state to a configured JDBC store. 

Apache Pekko Persistence JDBC requires Apache Pekko $pekko.version$ or later. It uses @extref:[Slick](slick:) $slick.version$ internally to access the database via JDBC, this does not require user code to make use of Slick.

## Module info

@@dependency [sbt,Maven,Gradle] {
  group=org.apache.pekko
  artifact=pekko-persistence-jdbc_$scala.binary.version$
  version=$project.version$
  symbol2=PekkoVersion
  value2=$pekko.version$
  group2=org.apache.pekko
  artifact2=pekko-persistence-query_$scala.binary.version$
  version2=PekkoVersion
  symbol3=SlickVersion
  value3=$slick.version$
  group3=com.typesafe.slick
  artifact3=slick_$scala.binary.version$
  version3=SlickVersion
  group4=com.typesafe.slick
  artifact4=slick-hikaricp_$scala.binary.version$
  version4=SlickVersion
}

@@project-info{ projectId="core" }

## Contribution policy

Contributions via GitHub pull requests are gladly accepted from their original author. Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## Code of Conduct

Contributors all agree to follow the [Apache Community Code of Conduct](https://www.apache.org/foundation/policies/conduct.html).

## License

This source code is made available under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0).
