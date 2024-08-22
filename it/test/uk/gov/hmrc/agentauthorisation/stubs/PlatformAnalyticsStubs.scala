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
    stubFor(
      post(urlPathMatching(platformAnalyticsUrl))
        .withRequestBody(matchingJsonPath("$[?(@.events.size() == 1)]"))
        .willReturn(aResponse().withStatus(200))
    )

  def verifyPlatformAnalyticsEventWasSent(action: String, label: Option[String]) = eventually {
    verify(
      1,
      postRequestedFor(urlPathEqualTo(platformAnalyticsUrl))
        .withRequestBody(similarToJson(s"""{
                                          |  "gaTrackingId": "token",
                                          |  "events": ${Json.toJson(
          List(Event("agent-authorisation-api", action, label.getOrElse(""), Seq.empty))
        )}
                                          |}"""))
    )
  }

  private def similarToJson(value: String) = equalToJson(value.stripMargin, true, true)

}
