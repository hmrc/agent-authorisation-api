import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  private val bootstrapVersion: String = "9.11.0"

  lazy val compileDeps: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "com.github.blemale"      %% "scaffeine"                  % "5.3.0",
    "uk.gov.hmrc"             %% "agent-mtd-identifiers"      % "2.2.0",
    "uk.gov.hmrc"             %% "play-hal-play-30"           % "4.0.0",
    "uk.gov.hmrc"             %% "play-hmrc-api-play-30"      % "8.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion  % Test,
    "org.scalamock"           %% "scalamock"                  % "7.3.0"           % Test
  )

}
