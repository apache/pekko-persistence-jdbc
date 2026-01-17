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
  val Scala3 = "3.3.7"
  val ScalaVersions = Seq(Scala213, Scala3)

  val PekkoVersion = PekkoCoreDependency.version

  val LogbackVersion = "1.5.24"

  val SlickVersion = "3.6.1"
  val SlickDocVersion = SlickVersion
  val ScalaTestVersion = "3.2.19"

  val JdbcDrivers = Seq(
    "org.postgresql" % "postgresql" % "42.7.9",
    "com.h2database" % "h2" % "2.4.240",
    "com.mysql" % "mysql-connector-j" % "9.5.0",
    "org.mariadb.jdbc" % "mariadb-java-client" % "3.5.7",
    "com.microsoft.sqlserver" % "mssql-jdbc" % "13.2.1.jre11",
    "com.oracle.database.jdbc" % "ojdbc8" % "23.26.0.0.0")

  val Libraries: Seq[ModuleID] = Seq(
    "com.typesafe.slick" %% "slick" % SlickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
    "ch.qos.logback" % "logback-classic" % LogbackVersion % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test) ++ JdbcDrivers.map(_ % Test)

  val Migration: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % "1.4.5",
    "ch.qos.logback" % "logback-classic" % LogbackVersion,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test) ++ JdbcDrivers.map(_ % Provided)
}
