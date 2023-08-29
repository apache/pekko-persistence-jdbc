/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import com.lightbend.paradox.apidoc.ApidocPlugin.autoImport.apidocRootPackage
import org.apache.pekko.PekkoParadoxPlugin.autoImport._
import net.bzzt.reproduciblebuilds.ReproducibleBuildsPlugin.reproducibleBuildsCheckResolver
import sbt.Keys._

ThisBuild / apacheSonatypeProjectProfile := "pekko"
ThisBuild / versionScheme := Some(VersionScheme.SemVerSpec)
sourceDistName := "apache-pekko-persistence-jdbc"
sourceDistIncubating := true

commands := commands.value.filterNot { command =>
  command.nameOption.exists { name =>
    name.contains("sonatypeRelease") || name.contains("sonatypeBundleRelease")
  }
}

ThisBuild / reproducibleBuildsCheckResolver :=
  "Apache Pekko Staging".at("https://repository.apache.org/content/groups/staging/")

lazy val `pekko-persistence-jdbc` = project
  .in(file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .disablePlugins(MimaPlugin, SitePlugin)
  .aggregate(core, docs, migrator)
  .settings(
    name := "pekko-persistence-jdbc-root",
    publish / skip := true)

lazy val core = project
  .in(file("core"))
  .enablePlugins(MimaPlugin, ReproducibleBuildsPlugin)
  .disablePlugins(SitePlugin)
  .configs(IntegrationTest.extend(Test))
  .settings(Defaults.itSettings)
  .settings(
    name := "pekko-persistence-jdbc",
    libraryDependencies ++= Dependencies.Libraries,
    mimaReportSignatureProblems := true,
    // temporarily disable mima checks
    mimaPreviousArtifacts := Set.empty)

lazy val migrator = project
  .in(file("migrator"))
  .disablePlugins(SitePlugin, MimaPlugin, ReproducibleBuildsPlugin)
  .configs(IntegrationTest.extend(Test))
  .settings(Defaults.itSettings)
  .settings(
    name := "pekko-persistence-jdbc-migrator",
    libraryDependencies ++= Dependencies.Migration ++ Dependencies.Libraries,
    // TODO remove this when ready to publish it
    publish / skip := true)
  .dependsOn(core % "compile->compile;test->test")

lazy val themeSettings = Seq(
  pekkoParadoxGithub := Some("https://github.com/apache/incubator-pekko-persistence-jdbc"))

lazy val docs = project
  .enablePlugins(ProjectAutoPlugin, PekkoParadoxPlugin, ParadoxSitePlugin, PreprocessPlugin)
  .disablePlugins(MimaPlugin)
  .settings(
    name := "Apache Pekko Persistence JDBC",
    publish / skip := true,
    makeSite := makeSite.dependsOn(LocalRootProject / ScalaUnidoc / doc).value,
    previewPath := (Paradox / siteSubdirName).value,
    Preprocess / siteSubdirName := s"api/pekko-persistence-jdbc/${if (isSnapshot.value) "snapshot"
      else version.value}",
    Preprocess / sourceDirectory := (LocalRootProject / ScalaUnidoc / unidoc / target).value,
    Paradox / siteSubdirName := s"docs/pekko-persistence-jdbc/${if (isSnapshot.value) "snapshot" else version.value}",
    Compile / paradoxProperties ++= Map(
      "project.url" -> "https://pekko.apache.org/docs/pekko-persistence-jdbc/current/",
      "github.base_url" -> "https://github.com/apache/incubator-pekko-persistence-jdbc/",
      "canonical.base_url" -> "https://pekko.apache.org/docs/pekko-persistence-jdbc/current",
      "pekko.version" -> "current",
      "slick.version" -> Dependencies.SlickVersion,
      "extref.github.base_url" -> s"https://github.com/apache/incubator-pekko-persistence-jdbc/blob/${if (isSnapshot.value) "master"
        else "v" + version.value}/%s",
      // Slick
      "extref.slick.base_url" -> s"https://scala-slick.org/doc/${Dependencies.SlickVersion}/%s",
      // Pekko
      "extref.pekko.base_url" -> "https://pekko.apache.org/docs/pekko/current/%s",
      "scaladoc.base_url" -> "https://pekko.apache.org/api/pekko-persistence-jdbc/current/",
      "scaladoc.org.apache.pekko.base_url" -> "https://pekko.apache.org/api/pekko/current/",
      "javadoc.org.apache.pekko.base_url" -> "https://pekko.apache.org/japi/pekko/current/",
      "javadoc.org.apache.pekko.link_style" -> "direct",
      // Java
      "javadoc.base_url" -> "https://docs.oracle.com/javase/8/docs/api/",
      // Scala
      "scaladoc.scala.base_url" -> s"https://www.scala-lang.org/api/${scalaBinaryVersion.value}.x/",
      "scaladoc.org.apache.pekko.persistence.jdbc.base_url" -> s"/${(Preprocess / siteSubdirName).value}/"),
    paradoxGroups := Map("Language" -> Seq("Java", "Scala")),
    resolvers += Resolver.jcenterRepo,
    apidocRootPackage := "org.apache.pekko")
  .settings(themeSettings)

Global / onLoad := (Global / onLoad).value.andThen { s =>
  val v = version.value
  if (dynverGitDescribeOutput.value.hasNoTags)
    sLog.value.warn(s"Failed to derive version from git tags. Maybe run `git fetch --unshallow`? Derived version: $v")
  s
}

TaskKey[Unit]("verifyCodeFmt") := {
  javafmtCheckAll.all(ScopeFilter(inAnyProject)).result.value.toEither.left.foreach { _ =>
    throw new MessageOnlyException(
      "Unformatted Java code found. Please run 'javafmtAll' and commit the reformatted code")
  }
}
