import org.typelevel.scalacoptions.ScalacOptions

organization := "works.perpetuum"
organizationName := "Perpetuum Works"
publishTo := Some(Resolver.file("local-deploy", file("target/deploy")))
pomIncludeRepository := { _ => false }
versionScheme := Some("early-semver")
homepage := Some(url("https://github.com/marmeblade/icu4scala"))
startYear := Some(2024)
licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
developers := List(
  Developer(
    "marmeblade",
    "David Brügmann",
    "marmeblade@gmail.com",
    url("https://github.com/marmeblade")
  )
)
scmInfo := Some(
  ScmInfo(
    url("https://github.com/marmeblade/icu4scala"),
    "scm:git@github.com:marmeblade/icu4scala.git"
  )
)

semanticdbEnabled := true
semanticdbVersion := scalafixSemanticdb.revision

val Scala3 = "3.8.4"
val Scala213 = "2.13.18"
val Scala212 = "2.12.21"

lazy val root = project
  .in(file("."))
  .aggregate(core.projectRefs*)
  .aggregate(docs)
  .aggregate(sbtPlugin.projectRefs*)
  .settings(
    publish / skip := true,
    publishLocal / skip := true
  )

lazy val core = projectMatrix
  .in(file("modules/core"))
  .settings(
    name := "icu4scala",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "fastparse" % "3.1.1",
      "com.lihaoyi" %% "sourcecode" % "0.4.4",
      "org.scalameta" %% "munit" % "1.3.3" % Test
    ),
    tpolecatExcludeOptions ++= Set(
      ScalacOptions.warnUnusedNoWarn,
      ScalacOptions.privateWarnUnusedNoWarn
    )
  )
  .jvmPlatform(Seq(Scala3, Scala212, Scala213))
  .jsPlatform(Seq(Scala3, Scala212, Scala213))
  .nativePlatform(Seq(Scala3, Scala212, Scala213))

lazy val docs = project
  .in(file("modules/icu4scala-docs"))
  .enablePlugins(MdocPlugin)
  .settings(
    scalaVersion := Scala213,
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    publish / skip := true,
    publishLocal / skip := true
  )
  .dependsOn(core.jvm(Scala213))

lazy val sbtPlugin = projectMatrix
  .enablePlugins(SbtPlugin)
  .in(file("modules/sbt-icu4scala"))
  .settings(
    name := "sbt-icu4scala",
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.11.7"
        case _      => "2.0.1"
      }
    },
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.3.3" % Test,
      "com.typesafe" % "config" % "1.4.9",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.14.0"
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    missinglinkIgnoreDestinationPackages ++= Seq(
      IgnoredPackage("org.osgi.framework"),
      IgnoredPackage("org.slf4j.ext"),
      IgnoredPackage("org.conscrypt")
    ),
    missinglinkExcludedDependencies ++= Seq(
      moduleFilter(organization = "org.scala-sbt", name = "librarymanagement-coursier_3"),
      moduleFilter(organization = "org.slf4j", name = "slf4j-api")
    )
  )
  .jvmPlatform(Seq(Scala212, Scala3))
  .dependsOn(
    core,
    core % "test->test"
  )

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
  "docs / mdoc",
  "scalafmtCheckAll",
  "scalafmtSbtCheck",
  s"scalafix --check $scalafixRules",
  "headerCheck",
  "missinglinkCheck"
).mkString(";")

val PrepareCICommands = Seq(
  s"scalafix --rules $scalafixRules",
  "scalafmtAll",
  "scalafmtSbt",
  "headerCreate"
).mkString(";")

addCommandAlias("ci", CICommands)
addCommandAlias("preCI", PrepareCICommands)
