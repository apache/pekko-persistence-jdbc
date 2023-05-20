/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import org.mdedetrich.apache.sonatype.ApacheSonatypePlugin
import ApacheSonatypePlugin.autoImport.apacheSonatypeDisclaimerFile
import sbtdynver.DynVerPlugin
import sbtdynver.DynVerPlugin.autoImport.dynverSonatypeSnapshots

object ProjectAutoPlugin extends AutoPlugin {
  object autoImport {}

  override val requires = JvmPlugin && ApacheSonatypePlugin && DynVerPlugin

  override def globalSettings =
    Seq(
      homepage := Some(url("https://pekko.apache.org/docs/pekko-persistence-jdbc/current/")),
      scmInfo := Some(
        ScmInfo(url("https://github.com/apache/incubator-pekko-persistence-jdbc"),
          "git@github.com:apache/incubator-pekko-persistence-jdbc.git")),
      developers += Developer(
        "contributors",
        "Contributors",
        "dev@pekko.apache.org",
        url("https://github.com/apache/incubator-pekko-persistence-jdbc/graphs/contributors")),
      description := "A plugin for storing events in an event journal pekko-persistence-jdbc",
      startYear := Some(2022))

  override val trigger: PluginTrigger = allRequirements

  override val projectSettings: Seq[Setting[_]] = Seq(
    crossVersion := CrossVersion.binary,
    crossScalaVersions := Dependencies.ScalaVersions,
    scalaVersion := Dependencies.Scala213,
    Test / fork := false,
    Test / parallelExecution := false,
    Test / logBuffered := true,
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-unchecked",
      "-Xlog-reflective-calls",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-release:8"),
    Compile / scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) =>
        disciplineScalacOptions -- Set(
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Ypartial-unification",
          "-Yno-adapted-args")
      case Some((2, 12)) =>
        disciplineScalacOptions
      case _ =>
        Nil
    }).toSeq,
    scalacOptions += "-Ydelambdafy:method",
    Compile / doc / scalacOptions := scalacOptions.value ++ Seq(
      "-doc-title",
      "Apache Pekko Persistence JDBC",
      "-doc-version",
      version.value,
      "-sourcepath",
      (ThisBuild / baseDirectory).value.toString,
      "-skip-packages",
      "pekko.pattern", // for some reason Scaladoc creates this
      "-doc-source-url", {
        val branch = if (isSnapshot.value) "master" else s"v${version.value}"
        s"https://github.com/apache/incubator-pekko-persistence-jdbc/tree/${branch}€{FILE_PATH_EXT}#L€{FILE_LINE}"
      },
      "-doc-canonical-base-url",
      "https://pekko.apache.org/api/pekko-persistence-jdbc/current/"),
    // show full stack traces and test case durations
    Test / testOptions += Tests.Argument("-oDF"),
    resolvers += Resolver.jcenterRepo,
    apacheSonatypeDisclaimerFile := Some((LocalRootProject / baseDirectory).value / "DISCLAIMER"))

  override lazy val buildSettings = Seq(
    dynverSonatypeSnapshots := true)

  val disciplineScalacOptions = Set(
//    "-Xfatal-warnings",
    "-feature",
    "-Yno-adapted-args",
    "-deprecation",
    "-Xlint",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-unused:_",
    "-Ypartial-unification",
    "-Ywarn-extra-implicit")

}
