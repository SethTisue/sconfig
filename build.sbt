// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.CrossPlugin.autoImport.crossProject

addCommandAlias(
  "run-examples",
  Seq(
    "sconfig-simple-app-scala/run",
    "sconfig-complex-app-scala/run",
    "sconfig-simple-app-java/run",
    "sconfig-complex-app-java/run"
  ).mkString(";", ";", "")
)

val scala212 = "2.12.8"
val scala211 = "2.11.12"
val scalaVersions = List(scala212, scala211)

val nextVersion = "0.8.0"
// stable snapshot is not great for publish local
def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val tag = out.ref.dropV.value
  if (out.isCleanAfterTag) tag
  else nextVersion + "-SNAPSHOT"
}

val scalacOpts = List("-unchecked", "-deprecation", "-feature")

ThisBuild / Compile / scalacOptions := scalacOpts
ThisBuild / Test / scalacOptions := scalacOpts

ThisBuild / crossScalaVersions := scalaVersions

inThisBuild(
  List(
    version := dynverGitDescribeOutput.value.mkVersion(versionFmt, ""),
    dynver := sbtdynver.DynVer
      .getGitDescribeOutput(new java.util.Date)
      .mkVersion(versionFmt, ""),
    description := "Configuration library for Scala using HOCON files",
    organization := "org.ekrich",
    homepage := Some(url("https://github.com/ekrich/sconfig")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        id = "ekrich",
        name = "Eric K Richardson",
        email = "ekrichardson@gmail.com",
        url = url("http://github.ekrich.org/")
      )
    )
  ))

ThisBuild / pomIncludeRepository := { _ =>
  false
}

lazy val root = (project in file("."))
  .aggregate(
    testLibJVM,
    sconfigJVM,
    //sconfigNative,
    simpleLibScala,
    simpleAppScala,
    complexAppScala,
    simpleLibJava,
    simpleAppJava,
    complexAppJava
  )
  .settings(commonSettings)
  .settings(
    name := "sconfig-root",
    crossScalaVersions := Nil,
    doc / aggregate := false,
    doc := (sconfigJVM / Compile / doc).value,
    packageDoc / aggregate := false,
    packageDoc := (sconfigJVM / Compile / packageDoc).value,
  )

lazy val sconfig = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full) // [Pure, Full, Dummy], default: CrossType.Full
  //.jsSettings(/* ... */) // defined in sbt-scalajs-crossproject
  .jvmSettings(
    crossScalaVersions := scalaVersions,
    libraryDependencies += {
      val liftVersion = scalaBinaryVersion.value match {
        case "2.10" => "2.6.3" // last version that supports 2.10
        case _      => "3.3.0" // latest version for 2.11 and 2.12
      }
      "net.liftweb" %% "lift-json" % liftVersion % Test
    },
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    Compile / compile / javacOptions ++= Seq("-source",
                                             "1.8",
                                             "-target",
                                             "1.8",
                                             "-g",
                                             "-Xlint:unchecked"),
    // because we test some global state such as singleton caches,
    // we have to run tests in serial.
    Test / parallelExecution := false,
    test / fork := true,
    Test / fork := true,
    run / fork := true,
    Test / run / fork := true,
    //env vars for tests
    Test / envVars ++= Map(
      "testList.0"      -> "0",
      "testList.1"      -> "1",
      "testClassesPath" -> (Test / classDirectory).value.getPath),
    // replace with your old artifact id
    mimaPreviousArtifacts := Set("org.ekrich" %% "sconfig" % "0.7.0"),
    mimaBinaryIssueFilters ++= ignoredABIProblems
  )
  .nativeSettings(
    //sources in Test := Nil,
    //nativeLinkStubs := true,
    scalaVersion := scala211,
    crossScalaVersions := List(scala211)
  )

lazy val sconfigJVM = sconfig.jvm
  .dependsOn(testLibJVM % "test->test")

lazy val sconfigNative = sconfig.native
  .settings(
    libraryDependencies += "com.lihaoyi" %%% "utest" % "0.6.6" % Test,
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val ignoredABIProblems = {
  import com.typesafe.tools.mima.core._
  import com.typesafe.tools.mima.core.ProblemFilters._
  Seq(
    exclude[Problem]("com.typesafe.config.impl.*")
  )
}

lazy val commonSettings: Seq[Setting[_]] =
  Def.settings(
    skipPublish
  )

def proj(id: String, base: File) =
  Project(id, base) settings commonSettings

lazy val testLibJVM = testLib.jvm

lazy val testLib = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("test-lib"))
  .settings(
    name := "sconfig-test-lib",
    crossScalaVersions := scalaVersions,
    publish / skip := true
  )

lazy val simpleLibScala = proj(
  "sconfig-simple-lib-scala",
  file("examples/scala/simple-lib")) dependsOn sconfigJVM
lazy val simpleAppScala = proj(
  "sconfig-simple-app-scala",
  file("examples/scala/simple-app")) dependsOn simpleLibScala
lazy val complexAppScala = proj(
  "sconfig-complex-app-scala",
  file("examples/scala/complex-app")) dependsOn simpleLibScala

lazy val simpleLibJava = proj(
  "sconfig-simple-lib-java",
  file("examples/java/simple-lib")) dependsOn sconfigJVM
lazy val simpleAppJava = proj(
  "sconfig-simple-app-java",
  file("examples/java/simple-app")) dependsOn simpleLibJava
lazy val complexAppJava = proj(
  "sconfig-complex-app-java",
  file("examples/java/complex-app")) dependsOn simpleLibJava

val skipPublish = Seq(
  // no artifacts in this project
  publishArtifact := false,
  // make-pom has a more specific publishArtifact setting already
  // so needs specific override
  makePom / publishArtifact := false,
  // no docs to publish
  packageDoc / publishArtifact := false,
  publish / skip := true
)
