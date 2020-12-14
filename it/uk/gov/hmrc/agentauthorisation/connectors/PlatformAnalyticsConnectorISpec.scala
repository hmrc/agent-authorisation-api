package uk.gov.hmrc.agentauthorisation.connectors

import akka.Done
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentauthorisation.models.{AnalyticsRequest, Event}
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class PlatformAnalyticsConnectorISpec extends BaseISpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val analyticsRequest = AnalyticsRequest(
    gaClientId = Some("foo"),
    gaTrackingId = Some("bar"),
      events = List(Event(
        category = "agent-authorisation-api",
          action = "action",
        label = "HMRC-MTD-IT",
        dimensions= Seq.empty))
  )

  val connector = app.injector.instanceOf[PlatformAnalyticsConnector]

  "PlatformAnalyticsConnector" should {
    "return Done when response is OK" in {

      givenAnalyticsStubResponse(200)
      val result = await(connector.sendEvent(analyticsRequest))
      result shouldBe Done
    }

    "return Done when response is 5xx" in {
      givenAnalyticsStubResponse(500)
      val result = await(connector.sendEvent(analyticsRequest))
      result shouldBe Done
    }
  }

  private val requestBodyJson = s"""{
                                   |"gaClientId": "foo",
                                   |"gaTrackingId": "bar",
                                   |"events": [
                                   |{
                                   |"category": "agent-authorisation-api",
                                   |"action": "action",
                                   |"label": "HMRC-MTD-IT",
                                   |"dimensions": []
                                   |}]
                                   |}""".stripMargin

  private def givenAnalyticsStubResponse(code: Int): StubMapping =
    stubFor(post(urlEqualTo("/platform-analytics/event"))
      .withRequestBody(equalToJson(
       requestBodyJson)).willReturn(aResponse().withStatus(code)))
}
