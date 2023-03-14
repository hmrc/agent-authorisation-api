import AppDependencies.{compileDeps, testDeps}
import CodeCoverageSettings.scoverageSettings

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-java8-compat" % "always"

val silencerVersion = "1.7.8"

lazy val root = (project in file("."))
  .settings(
    name := "agent-authorisation-api",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.15",
    PlayKeys.playDefaultPort := 9433,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
    ),
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    ),
    routesImport += "uk.gov.hmrc.agentauthorisation.binders.UrlBinders._",
    routesImport -= "controllers.Assets.Asset",
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    majorVersion := 0,
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Ypartial-unification",
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-P:silencer:pathFilters=views;routes")
  )
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := false,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)

inConfig(IntegrationTest)(scalafmtCoreSettings)
