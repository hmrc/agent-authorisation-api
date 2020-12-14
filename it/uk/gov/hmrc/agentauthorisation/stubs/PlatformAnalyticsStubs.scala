package uk.gov.hmrc.agentauthorisation.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.Eventually.eventually
import play.api.libs.json.Json
import uk.gov.hmrc.agentauthorisation.models.Event
import uk.gov.hmrc.agentauthorisation.support.WireMockSupport

trait PlatformAnalyticsStubs {
  me: WireMockSupport =>

  private val platformAnalyticsUrl = "/platform-analytics/event"

  def givenPlatformAnalyticsEventWasSent(): StubMapping =
    stubFor(post(urlPathMatching(platformAnalyticsUrl))
      .withRequestBody(matchingJsonPath({"$[?(@.events.size() == 1)]"}))
      .willReturn(aResponse().withStatus(200)))


  def verifyPlatformAnalyticsEventWasSent(action: String, label: Option[String]) =  eventually {
    verify(
      1,
      postRequestedFor(urlPathEqualTo(platformAnalyticsUrl))
        .withRequestBody(similarToJson(s"""{
                                          |  "gaTrackingId": "token",
                                          |  "events": ${Json.toJson(List(Event("agent-authorisation-api",action,label.getOrElse(""),Seq.empty)))}
                                          |}"""))
    )
  }

  private def similarToJson(value: String) = equalToJson(value.stripMargin, true, true)

}
