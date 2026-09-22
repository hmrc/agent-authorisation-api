/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.actions

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models.ApiVersion
import uk.gov.hmrc.agentauthorisation.support.UnitSpec

import scala.concurrent.Future

class ApiVersionActionSpec extends UnitSpec with MockitoSugar:

  private def selectedVersion(v3Enabled: Boolean, acceptHeader: Option[String]): ApiVersion =
    val appConfig = mock[AppConfig]
    when(appConfig.v3Enabled).thenReturn(v3Enabled)

    val action = ApiVersionAction(appConfig, stubControllerComponents())
    val request = acceptHeader.fold(FakeRequest())(value => FakeRequest().withHeaders(HeaderNames.ACCEPT -> value))

    val result = action.invokeBlock(
      request,
      versionedRequest => Future.successful(Ok(versionedRequest.apiVersion.toString))
    ).futureValue

    ApiVersion.valueOf(contentAsString(result))

  "ApiVersionAction" should:
    "select the V1/V2 handler for every header when the V3 feature is disabled" in:
      val acceptHeaders = Seq(
        None,
        Some("application/vnd.hmrc.1.0+json"),
        Some("application/vnd.hmrc.2.0+json"),
        Some(ApiVersion.V3AcceptHeader),
        Some("application/json")
      )

      acceptHeaders.foreach: acceptHeader =>
        selectedVersion(v3Enabled = false, acceptHeader) shouldBe ApiVersion.V1V2

    "select the V3 handler for the exact V3 Accept header when the feature is enabled" in:
      selectedVersion(v3Enabled = true, Some(ApiVersion.V3AcceptHeader)) shouldBe ApiVersion.V3

    "select the V1/V2 handler for every other header when the V3 feature is enabled" in:
      val acceptHeaders = Seq(
        None,
        Some("application/vnd.hmrc.1.0+json"),
        Some("application/vnd.hmrc.2.0+json"),
        Some("application/json"),
        Some(s"${ApiVersion.V3AcceptHeader}; charset=utf-8"),
        Some(ApiVersion.V3AcceptHeader.toUpperCase),
        Some(s"${ApiVersion.V3AcceptHeader}, application/json")
      )

      acceptHeaders.foreach: acceptHeader =>
        selectedVersion(v3Enabled = true, acceptHeader) shouldBe ApiVersion.V1V2
