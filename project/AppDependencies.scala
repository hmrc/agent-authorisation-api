import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {
  private val mongoVer = "2.6.0"
  private val bootstrapVersion: String = "9.12.0"

  lazy val compileDeps: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "com.github.blemale"      %% "scaffeine"                  % "5.3.0",
    "uk.gov.hmrc"             %% "agent-mtd-identifiers"      % "2.2.0",
    "uk.gov.hmrc"             %% "play-hal-play-30"           % "4.0.0",
    "uk.gov.hmrc"             %% "play-hmrc-api-play-30"      % "8.0.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % mongoVer
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion  % Test,
    "org.scalamock"           %% "scalamock"                  % "7.3.1"           % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % mongoVer          % Test
  )

}
