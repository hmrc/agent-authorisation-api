package uk.gov.hmrc.agentauthorisation.support

import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentauthorisation.stubs._
import uk.gov.hmrc.play.test.UnitSpec

abstract class BaseISpec
  extends UnitSpec with OneAppPerSuite with WireMockSupport with AuthStubs with ACAStubs {

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.agent-client-authorisation.port" -> wireMockPort,
        "passcodeAuthentication.enabled" -> true)

  protected implicit val materializer = app.materializer

}
