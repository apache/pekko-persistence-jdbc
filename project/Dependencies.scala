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
  val Scala212 = "2.12.19"
  val Scala213 = "2.13.14"
  val Scala3 = "3.3.3"
  val ScalaVersions = Seq(Scala212, Scala213, Scala3)

  val PekkoVersion = PekkoCoreDependency.version

  val LogbackVersion = "1.3.14"

  val SlickVersion = "3.5.1"
  val ScalaTestVersion = "3.2.19"

  val JdbcDrivers = Seq(
    "org.postgresql" % "postgresql" % "42.7.4",
    "com.h2database" % "h2" % "2.2.224",
    "com.mysql" % "mysql-connector-j" % "9.0.0",
    "com.microsoft.sqlserver" % "mssql-jdbc" % "12.8.1.jre8",
    "com.oracle.database.jdbc" % "ojdbc8" % "23.5.0.24.07")

  val Libraries: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-persistence-query" % PekkoVersion,
    "com.typesafe.slick" %% "slick" % SlickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
    "ch.qos.logback" % "logback-classic" % LogbackVersion % Test,
    "org.apache.pekko" %% "pekko-slf4j" % PekkoVersion % Test,
    "org.apache.pekko" %% "pekko-persistence-tck" % PekkoVersion % Test,
    "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test,
    "org.apache.pekko" %% "pekko-testkit" % PekkoVersion % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test) ++ JdbcDrivers.map(_ % Test)

  val Migration: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % "1.4.3",
    "ch.qos.logback" % "logback-classic" % LogbackVersion,
    "org.testcontainers" % "postgresql" % "1.20.1" % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test) ++ JdbcDrivers.map(_ % Provided)
}
