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
  val Scala212 = "2.12.18"
  val Scala213 = "2.13.12"
  val Scala3 = "3.3.3"
  val ScalaVersions = Seq(Scala212, Scala213, Scala3)

  val PekkoVersion = PekkoCoreDependency.version

  val SlickVersion = "3.5.0"
  val ScalaTestVersion = "3.2.18"

  val JdbcDrivers = Seq(
    "org.postgresql" % "postgresql" % "42.7.1",
    "com.h2database" % "h2" % "2.2.224",
    "com.mysql" % "mysql-connector-j" % "8.3.0",
    "com.microsoft.sqlserver" % "mssql-jdbc" % "8.4.1.jre8",
    "com.oracle.database.jdbc" % "ojdbc6" % "11.2.0.4")

  val Libraries: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-persistence-query" % PekkoVersion,
    "com.typesafe.slick" %% "slick" % SlickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.13" % Test,
    "org.apache.pekko" %% "pekko-slf4j" % PekkoVersion % Test,
    "org.apache.pekko" %% "pekko-persistence-tck" % PekkoVersion % Test,
    "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test,
    "org.apache.pekko" %% "pekko-testkit" % PekkoVersion % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test) ++ JdbcDrivers.map(_ % Test)

  val Migration: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % "1.4.3",
    "ch.qos.logback" % "logback-classic" % "1.2.13",
    "org.testcontainers" % "postgresql" % "1.16.3" % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test) ++ JdbcDrivers.map(_ % Provided)
}
