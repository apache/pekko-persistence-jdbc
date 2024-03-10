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
  val Scala213 = "2.13.13"
  val ScalaVersions = Seq(Scala212, Scala213)

  val PekkoVersion = "1.0.1"

  val SlickVersion = "3.3.3"
  val ScalaTestVersion = "3.2.14"

  val JdbcDrivers = Seq(
    "org.postgresql" % "postgresql" % "42.3.8",
    "com.h2database" % "h2" % "2.2.220",
    "mysql" % "mysql-connector-java" % "8.0.28",
    "com.microsoft.sqlserver" % "mssql-jdbc" % "7.4.1.jre8",
    "com.oracle.database.jdbc" % "ojdbc6" % "11.2.0.4")

  val Libraries: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-persistence-query" % PekkoVersion,
    "com.typesafe.slick" %% "slick" % SlickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.10" % Test,
    "org.apache.pekko" %% "pekko-slf4j" % PekkoVersion % Test,
    "org.apache.pekko" %% "pekko-persistence-tck" % PekkoVersion % Test,
    "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test,
    "org.apache.pekko" %% "pekko-testkit" % PekkoVersion % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test) ++ JdbcDrivers.map(_ % Test)

  val Migration: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % "1.4.2",
    "ch.qos.logback" % "logback-classic" % "1.2.13",
    "org.testcontainers" % "postgresql" % "1.16.3" % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test) ++ JdbcDrivers.map(_ % Provided)
}
