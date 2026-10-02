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
import play.api.mvc.ControllerComponents
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models.{ApiVersion, NoPermissionOnAgency, NotAnAgent, StandardNotFound}
import uk.gov.hmrc.agentauthorisation.support.BaseISpec

import scala.concurrent.Future

class VersionedAgentActionISpec extends BaseISpec with MockitoSugar:

  private val controllerComponents = app.injector.instanceOf[ControllerComponents]
  private val authorisedAgentAction = app.injector.instanceOf[AuthorisedAgentAction]

  private def invoke(v3Enabled: Boolean, acceptHeader: Option[String]) =
    val appConfig = mock[AppConfig]
    when(appConfig.v3Enabled).thenReturn(v3Enabled)
    val versionedAction = VersionedAgentAction(
      controllerComponents,
      ApiVersionAction(appConfig, controllerComponents),
      authorisedAgentAction
    )
    val request = acceptHeader.fold(
      FakeRequest().withHeaders("Authorization" -> "Bearer XYZ")
    )(value => FakeRequest().withHeaders("Authorization" -> "Bearer XYZ", "Accept" -> value))

    versionedAction(arn)(
      _ => Future.successful(Ok("legacy")),
      _ => Future.successful(Ok("v3"))
    )(request).futureValue

  private def invokeV3Only(v3Enabled: Boolean, acceptHeader: Option[String]) =
    val appConfig = mock[AppConfig]
    when(appConfig.v3Enabled).thenReturn(v3Enabled)
    val versionedAction = VersionedAgentAction(
      controllerComponents,
      ApiVersionAction(appConfig, controllerComponents),
      authorisedAgentAction
    )
    val request = acceptHeader.fold(
      FakeRequest().withHeaders("Authorization" -> "Bearer XYZ")
    )(value => FakeRequest().withHeaders("Authorization" -> "Bearer XYZ", "Accept" -> value))

    versionedAction.v3Only(arn)(_ => Future.successful(Ok("v3")))(request).futureValue

  "VersionedAgentAction" should:
    "invoke the legacy handler for the V3 header when the switch is off" in:
      givenAuthorisedAsValidAgent(arn.value)

      contentAsString(invoke(v3Enabled = false, Some(ApiVersion.V3AcceptHeader))) shouldBe "legacy"

    "invoke the V3 handler only for the exact header with the switch on" in:
      givenAuthorisedAsValidAgent(arn.value)

      contentAsString(invoke(v3Enabled = true, Some(ApiVersion.V3AcceptHeader))) shouldBe "v3"

    "pass the authenticated ARN and selected version to the handler" in:
      givenAuthorisedAsValidAgent(arn.value)
      val appConfig = mock[AppConfig]
      when(appConfig.v3Enabled).thenReturn(true)
      val action = VersionedAgentAction(
        controllerComponents,
        ApiVersionAction(appConfig, controllerComponents),
        authorisedAgentAction
      )

      val result = action(arn)(
        _ => Future.successful(Ok("legacy")),
        request => Future.successful(Ok(s"${request.arn.value}:${request.apiVersion}"))
      )(FakeRequest().withHeaders("Authorization" -> "Bearer XYZ", "Accept" -> ApiVersion.V3AcceptHeader)).futureValue

      contentAsString(result) shouldBe s"${arn.value}:V3"

    "invoke the legacy handler for other or missing headers with the switch on" in:
      Seq(None, Some("application/vnd.hmrc.2.0+json"), Some("application/json")).foreach: accept =>
        givenAuthorisedAsValidAgent(arn.value)
        contentAsString(invoke(v3Enabled = true, accept)) shouldBe "legacy"

    "reject an ARN mismatch before either handler runs" in:
      givenAuthorisedAsValidAgent(arn2.value)

      val result = invoke(v3Enabled = true, Some(ApiVersion.V3AcceptHeader))

      status(result) shouldBe 403
      contentAsJson(result) shouldBe NoPermissionOnAgency.toJson

    "reject a non-agent before either handler runs" in:
      givenUnauthorisedForInsufficientEnrolments()

      val result = invoke(v3Enabled = true, Some(ApiVersion.V3AcceptHeader))

      status(result) shouldBe 403
      contentAsJson(result) shouldBe NotAnAgent.toJson

  "VersionedAgentAction.v3Only" should:
    "invoke the handler only for the exact V3 header with the switch on" in:
      givenAuthorisedAsValidAgent(arn.value)

      contentAsString(invokeV3Only(v3Enabled = true, Some(ApiVersion.V3AcceptHeader))) shouldBe "v3"

    "return NOT_FOUND before authorisation when the switch is off" in:
      val result = invokeV3Only(v3Enabled = false, Some(ApiVersion.V3AcceptHeader))

      status(result) shouldBe 404
      contentAsJson(result) shouldBe StandardNotFound.toJson

    "return NOT_FOUND before authorisation for other or missing headers" in:
      Seq(None, Some("application/vnd.hmrc.2.0+json"), Some("application/json")).foreach: accept =>
        val result = invokeV3Only(v3Enabled = true, accept)

        status(result) shouldBe 404
        contentAsJson(result) shouldBe StandardNotFound.toJson
