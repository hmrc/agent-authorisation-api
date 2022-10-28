import play.core.PlayVersion
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-java8-compat" % "always"

val silencerVersion = "1.7.8"

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 80.00,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "7.8.0",
  "com.github.blemale" %% "scaffeine" % "5.1.1",
  "uk.gov.hmrc" %% "agent-kenshoo-monitoring" % "4.8.0-play-28",
  "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.47.0-play-28",
  "uk.gov.hmrc" %% "play-allowlist-filter" % "1.1.0",
  "uk.gov.hmrc" %% "play-hal" % "3.1.0-play-28",
  "uk.gov.hmrc" %% "play-hmrc-api" % "7.0.0-play-28"
)

def testDeps(scope: String) = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
  "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0" % scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.scalamock" %% "scalamock" % "4.4.0" % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.26.2" % scope,
  "com.vladsch.flexmark" %  "flexmark-all" % "0.35.10" % scope
)

lazy val root = (project in file("."))
  .settings(
    name := "agent-authorisation-api",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.15",
    PlayKeys.playDefaultPort := 9433,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
    ),
    resolvers += "HMRC-local-artefacts-maven" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases-local",
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    ),
    routesImport += "uk.gov.hmrc.agentauthorisation.binders.UrlBinders._",
    routesImport -= "controllers.Assets.Asset",
    publishingSettings,
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
