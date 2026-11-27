/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import sbt._

object Dependencies {
  // Keep in sync with .github CI build
  val Scala213 = "2.13.18"
  val Scala3 = "3.3.9"
  val Scala3Next = "3.9.0"
  // Scala3Next is only used for CI testing (`sbt ++3.9.0!` in h2-test.yml). It must not be in
  // ScalaVersions: `+publish` would publish it over the Scala 3.3 `_3` artifacts.
  val ScalaVersions = Seq(Scala213, Scala3)

  val PekkoVersion = PekkoCoreDependency.version

  val LogbackVersion = "1.6.8"

  val SlickVersion = "3.6.1"
  val SlickDocVersion = SlickVersion
  val ScalaTestVersion = "3.2.20"

  val JdbcDrivers = Seq(
    "org.postgresql" % "postgresql" % "42.7.15",
    "com.h2database" % "h2" % "2.5.252",
    "com.mysql" % "mysql-connector-j" % "26.10.0",
    "org.mariadb.jdbc" % "mariadb-java-client" % "4.0.0",
    "com.microsoft.sqlserver" % "mssql-jdbc" % "13.6.3.jre11",
    "com.oracle.database.jdbc" % "ojdbc11" % "23.26.4.0.0")

  val Libraries: Seq[ModuleID] = Seq(
    "com.typesafe.slick" %% "slick" % SlickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
    "ch.qos.logback" % "logback-classic" % LogbackVersion % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test) ++ JdbcDrivers.map(_ % Test)
}
