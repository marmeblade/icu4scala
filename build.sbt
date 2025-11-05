import org.typelevel.scalacoptions.ScalacOptions

Global / excludeLintKeys += logManager
Global / excludeLintKeys += scalaJSUseMainModuleInitializer
Global / excludeLintKeys += scalaJSLinkerConfig

inThisBuild(
  List(
    version := "0.1.0",
    organization := "works.perpetuum",
    organizationName := "Perpetuum Works",
    versionScheme := Some("early-semver"),
    homepage := Some(
      url("https://github.com/marmeblade/icu4scala")
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/marmeblade/icu4scala"),
        "scm:git@github.com:marmeblade/icu4scala.git"
      )
    ),
    startYear := Some(2024),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "marmeblade",
        "David Brügmann",
        "marmeblade@gmail.com",
        url("https://github.com/marmeblade")
      )
    ),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

// https://github.com/cb372/sbt-explicit-dependencies/issues/27
lazy val disableDependencyChecks = Seq(
  unusedCompileDependenciesTest := {},
  missinglinkCheck := {},
  undeclaredCompileDependenciesTest := {}
)

val Scala213 = "2.13.17"
val Scala212 = "2.12.20"
val Scala3 = "3.3.6"
val scalaVersions = Seq(Scala3, Scala212, Scala213)

lazy val munitSettings = Seq(
  libraryDependencies += {
    "org.scalameta" %%% "munit" % "1.0.2" % Test
  },
  testFrameworks += new TestFramework("munit.Framework")
)

lazy val root = project
  .aggregate(core.projectRefs*)
  .aggregate(sbtPlugin)
  .settings(
    name := "icu4scala-build",
    publish / skip := true,
    publishLocal / skip := true
  )

lazy val core = projectMatrix
  .in(file("modules/core"))
  .settings(
    name := "icu4scala",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "fastparse" % "3.1.1",
      "com.lihaoyi" %%% "sourcecode" % "0.4.0"
    )
  )
  .settings(munitSettings)
  .jvmPlatform(scalaVersions)
  .jsPlatform(scalaVersions, disableDependencyChecks)
  .nativePlatform(scalaVersions, disableDependencyChecks)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "icu4scala.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      scalaVersion,
      scalaBinaryVersion
    ),
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    tpolecatExcludeOptions ++= Set(
      ScalacOptions.warnUnusedNoWarn,
      ScalacOptions.privateWarnUnusedNoWarn
    )
  )

lazy val sbtPlugin = project
  .enablePlugins(SbtPlugin)
  .in(file("modules/sbt-icu4scala"))
  .settings(munitSettings)
  .settings(
    name := "sbt-icu4scala",
    scalaVersion := Scala212,
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.5",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.14.0",
      "org.scala-sbt" %% "collections" % "1.11.7",
      "org.scala-sbt" %% "command" % "1.11.7",
      "org.scala-sbt" %% "core-macros" % "1.11.7",
      "org.scala-sbt" %% "io" % "1.10.5",
      "org.scala-sbt" %% "librarymanagement-core" % "1.11.6",
      "org.scala-sbt" %% "main" % "1.11.7",
      "org.scala-sbt" %% "main-settings" % "1.11.7",
      "org.scala-sbt" % "sbt" % "1.11.7",
      "org.scala-sbt" %% "task-system" % "1.11.7",
      "org.scala-sbt" %% "util-logging" % "1.11.7",
      "org.scala-sbt" %% "util-position" % "1.11.7",
      "org.scala-sbt" %% "util-tracking" % "1.11.7"
    ),
    missinglinkIgnoreDestinationPackages ++= Seq(
      IgnoredPackage("org.osgi.framework"),
      IgnoredPackage("org.slf4j.ext"),
      IgnoredPackage("org.conscrypt")
    )
  )
  .dependsOn(
    core.jvm(Scala212),
    core.jvm(Scala212) % "test->test"
  )

lazy val docs = project
  .in(file("modules/icu4scala-docs"))
  .settings(
    scalaVersion := Scala213,
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    publish / skip := true,
    publishLocal / skip := true
  )
  .settings(disableDependencyChecks)
  .dependsOn(core.jvm(Scala213))
  .enablePlugins(MdocPlugin)

val scalafixRules = Seq(
  "OrganizeImports",
  "DisableSyntax",
  "LeakingImplicitClassVal",
  "NoValInForComprehension"
).mkString(" ")

val CICommands = Seq(
  "clean",
  "scalafixEnable",
  "compile",
  "test",
  "docs/mdoc",
  "scalafmtCheckAll",
  "scalafmtSbtCheck",
  s"scalafix --check $scalafixRules",
  "headerCheck",
  "undeclaredCompileDependenciesTest",
  "unusedCompileDependenciesTest",
  "missinglinkCheck"
).mkString(";")

val PrepareCICommands = Seq(
  s"scalafix --rules $scalafixRules",
  "scalafmtAll",
  "scalafmtSbt",
  "headerCreate",
  "undeclaredCompileDependenciesTest"
).mkString(";")

addCommandAlias("ci", CICommands)

addCommandAlias("preCI", PrepareCICommands)
