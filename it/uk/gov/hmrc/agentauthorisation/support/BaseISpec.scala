package uk.gov.hmrc.agentauthorisation.support

import akka.stream.Materializer
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentauthorisation.stubs._
import uk.gov.hmrc.play.test.UnitSpec

abstract class BaseISpec
    extends UnitSpec with OneAppPerSuite with WireMockSupport with AuthStubs with ACAStubs with DataStreamStubs
    with ACRStubs with DesStubs with TestIdentifiers {
  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "auditing.enabled"                                              -> true,
        "auditing.consumer.baseUri.host"                                -> wireMockHost,
        "auditing.consumer.baseUri.port"                                -> wireMockPort,
        "microservice.services.auth.port"                               -> wireMockPort,
        "microservice.services.agent-client-authorisation.port"         -> wireMockPort,
        "microservice.services.agent-client-relationships.port"         -> wireMockPort,
        "microservice.services.agent-client-relationships.host"         -> wireMockHost,
        "microservice.services.des.host"                                -> wireMockHost,
        "microservice.services.des.port"                                -> wireMockPort,
        "passcodeAuthentication.enabled"                                -> true,
        "api.supported-versions"                                        -> Seq("1.0")
      )

  protected implicit val materializer: Materializer = app.materializer

  def commonStubs(): Unit =
    givenAuditConnector()

  override protected def beforeEach(): Unit =
    super.beforeEach()
}
