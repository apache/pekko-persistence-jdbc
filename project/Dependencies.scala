import sbt._

object Dependencies {
  // Keep in sync with .github CI build
  val Scala212 = "2.12.16"
  val Scala213 = "2.13.8"
  val ScalaVersions = Seq(Scala212, Scala213)

  val PekkoVersion = "0.0.0+26546-767209a8-SNAPSHOT"

  val SlickVersion = "3.3.3"
  val ScalaTestVersion = "3.2.10"

  val JdbcDrivers = Seq(
    "org.postgresql" % "postgresql" % "42.3.3",
    "com.h2database" % "h2" % "2.1.212",
    "mysql" % "mysql-connector-java" % "8.0.28",
    "com.microsoft.sqlserver" % "mssql-jdbc" % "7.4.1.jre8")

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
    "ch.qos.logback" % "logback-classic" % "1.2.10",
    "org.testcontainers" % "postgresql" % "1.16.3" % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test) ++ JdbcDrivers.map(_ % Provided)
}
