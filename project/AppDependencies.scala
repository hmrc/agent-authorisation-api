import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val bootstrapVersion: String = "8.6.0"

  lazy val compileDeps = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "com.github.blemale"      %% "scaffeine"                  % "5.3.0",
    "uk.gov.hmrc"             %% "agent-mtd-identifiers"      % "2.2.0",
    "uk.gov.hmrc"             %% "play-hal-play-30"           % "4.0.0",
    "uk.gov.hmrc"             %% "play-hmrc-api-play-30"      % "8.0.0"
  )

  val test  = Seq(
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "7.0.1"           % Test,
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion  % Test,
    "org.pegdown"             % "pegdown"                     % "1.6.0"           % Test,
    "org.scalamock"           %% "scalamock"                  % "7.3.0"           % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.64.8"          % Test
  )

}
