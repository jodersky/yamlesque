// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}

val testSettings = Seq(
  libraryDependencies += "com.lihaoyi" %%% "utest" % "0.6.4" % "test",
  testFrameworks += new TestFramework("utest.runner.Framework")
)

lazy val yamlesque = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .settings(testSettings)
  .settings(
    scalaVersion := crossScalaVersions.value.head
  )
  .jvmSettings(
    crossScalaVersions := "2.12.4" :: "2.11.12" :: Nil
  )
  .jsSettings(
    crossScalaVersions := "2.12.4" :: "2.11.12" :: Nil
  )
  .nativeSettings(
    crossScalaVersions := "2.11.12" :: Nil,
    nativeLinkStubs := true // required for utest
  )

lazy val yamlesqueJVM    = yamlesque.jvm
lazy val yamlesqueJS     = yamlesque.js
lazy val yamlesqueNative = yamlesque.native

lazy val `yamlesque-spray-json` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(yamlesque)
  .settings(testSettings)
  .settings(
    libraryDependencies += "io.spray" %%% "spray-json" % "1.3.4"
  )
lazy val `yamlesque-spray-jsonJVM` = `yamlesque-spray-json`.jvm

lazy val root = (project in file("."))
  .aggregate(
    yamlesqueJVM,
    yamlesqueJS,
    yamlesqueNative,
    `yamlesque-spray-jsonJVM`
  )
  .settings(
    publish := {},
    publishLocal := {}
  )
