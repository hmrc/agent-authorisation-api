import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {
  private val mongoVer = "2.10.0"
  private val bootstrapVersion: String = "10.2.0"

  lazy val compileDeps: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"        %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "com.github.blemale" %% "scaffeine"                  % "5.3.0",
    "uk.gov.hmrc"        %% "play-hal-play-30"           % "4.0.0",
    "uk.gov.hmrc"        %% "play-hmrc-api-play-30"      % "8.0.0",
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-30"         % mongoVer,
    "uk.gov.hmrc"        %% "domain-play-30"             % "11.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapVersion % Test,
    "org.scalamock"     %% "scalamock"               % "7.4.1"          % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % mongoVer         % Test,
    "org.scalacheck"    %% "scalacheck"              % "1.18.1"         % Test
  )

}
