import AppDependencies.{compileDeps, test}
import CodeCoverageSettings.scoverageSettings
import uk.gov.hmrc.DefaultBuildSettings


val appName = "agent-authorisation-api"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.12"

val scalaCOptions = Seq(
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

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-java8-compat" % "always"


lazy val root = (project in file("."))
  .settings(
    name := appName,
    organization := "uk.gov.hmrc",
    PlayKeys.playDefaultPort := 9433,
    resolvers ++= Seq(Resolver.typesafeRepo("releases")),
    libraryDependencies ++= compileDeps ++ test,
    routesImport += "uk.gov.hmrc.agentauthorisation.binders.UrlBinders._",
    routesImport -= "controllers.Assets.Asset",
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    majorVersion := 0,
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    scalacOptions ++= scalaCOptions
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )