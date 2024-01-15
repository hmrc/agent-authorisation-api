import AppDependencies.{compileDeps, testDeps}
import CodeCoverageSettings.scoverageSettings
import sbt.IntegrationTest

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-java8-compat" % "always"


lazy val root = (project in file("."))
  .settings(
    name := "agent-authorisation-api",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.13.10",
    PlayKeys.playDefaultPort := 9433,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
    ),
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    routesImport += "uk.gov.hmrc.agentauthorisation.binders.UrlBinders._",
    routesImport -= "controllers.Assets.Asset",
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    majorVersion := 0,
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-Wconf:src=target/.*:s", // silence warnings from compiled files
      "-Wconf:src=*routes:s", // silence warnings from routes files
      "-Wconf:src=*html:w", // silence html warnings as they are wrong
    )
  )
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := false,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / scalafmtOnCompile := true
  )
  .settings(
    //fix for scoverage compile errors for scala 2.13.10
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)