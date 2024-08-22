/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.uk.gov.hmrc.agentauthorisation.connectors

import org.apache.pekko.Done
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.test.Helpers._
import uk.gov.hmrc.agentauthorisation.connectors.PlatformAnalyticsConnector
import uk.gov.hmrc.agentauthorisation.models.{AnalyticsRequest, Event}
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class PlatformAnalyticsConnectorISpec extends BaseISpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val analyticsRequest = AnalyticsRequest(
    gaClientId = Some("foo"),
    gaTrackingId = Some("bar"),
    events = List(
      Event(category = "agent-authorisation-api", action = "action", label = "HMRC-MTD-IT", dimensions = Seq.empty)
    )
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
    stubFor(
      post(urlEqualTo("/platform-analytics/event"))
        .withRequestBody(equalToJson(requestBodyJson))
        .willReturn(aResponse().withStatus(code))
    )
}
