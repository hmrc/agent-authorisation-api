import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {
  private val mongoVer: String = "2.12.0"
  private val bootstrapVersion: String = "10.7.0"
  private val playVer: String = "play-30"

  lazy val compileDeps: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"        %% s"bootstrap-frontend-$playVer" % bootstrapVersion,
    "uk.gov.hmrc"        %% s"bootstrap-backend-$playVer"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"  %% s"hmrc-mongo-$playVer"         % mongoVer,
    "uk.gov.hmrc"        %% s"play-hmrc-api-$playVer"      % "8.0.0",
    "uk.gov.hmrc"        %% s"play-hal-$playVer"           % "4.0.0",
    "uk.gov.hmrc"        %% s"domain-$playVer"             % "11.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVer"  % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVer" % mongoVer,
    "org.scalamock"     %% "scalamock"               % "7.5.5",
    "org.scalacheck"    %% "scalacheck"              % "1.19.0"
  ).map(_ % Test)

}
