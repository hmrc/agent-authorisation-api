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

package uk.gov.hmrc.agentauthorisation.controllers

import com.github.tomakehurst.wiremock.client.WireMock.*
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, Resource}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import scala.concurrent.ExecutionContext.Implicits.global

class CancelInvitationV3ControllerISpec extends BaseISpec:

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> true)

  private given WSClient = app.injector.instanceOf[WSClient]

  private def patch(
    authenticatedArn: String = arn.value,
    routeArn: String = arn.value,
    invitationId: String = invitationIdITSA.value
  ) =
    givenAuthorisedAsValidAgent(authenticatedArn)
    given HeaderCarrier = HeaderCarrier(
      authorization = Some(Authorization("Bearer XYZ"))
    )

    new Resource(s"/agents/$routeArn/invitations/$invitationId", port)
      .patchEmpty(Seq("Accept" -> ApiVersion.V3AcceptHeader))

  "PATCH /agents/:arn/invitations/:invitationId for V3" should:
    "return 204 without requiring a request body or Content-Type header" in:
      givenCancelAgentInvitationStub(invitationIdITSA, 204)

      val response = patch()

      response.status shouldBe 204
      response.body shouldBe empty
      verify(
        putRequestedFor(
          urlEqualTo(s"/agent-client-relationships/agent/cancel-invitation/${invitationIdITSA.value}")
        )
      )

    Seq(
      InvalidInvitationStatus,
      NoPermissionOnAgency,
      InvitationNotFound
    ).foreach: expectedError =>
      s"return ${expectedError.statusCode} ${expectedError.code} when ACR returns it" in:
        givenCancelAgentInvitationStubInvalid(expectedError, invitationIdITSA)

        val response = patch()

        response.status shouldBe expectedError.statusCode
        response.json shouldBe expectedError.toJson

    "reject a route ARN that differs from the authenticated ARN before calling ACR" in:
      val response = patch(authenticatedArn = arn2.value)

      response.status shouldBe 403
      response.json shouldBe NoPermissionOnAgency.toJson
      verify(0, putRequestedFor(urlPathMatching(".*/cancel-invitation/.*")))

    "return ARN_FORMAT_INVALID for an invalid route ARN" in:
      val response = patch(routeArn = "not-an-arn")

      response.status shouldBe 400
      response.json shouldBe ArnInvalidFormat.toJson

    "return INVITATION_ID_FORMAT_INVALID for an invalid invitation ID" in:
      val response = patch(invitationId = "not-an-invitation-id")

      response.status shouldBe 400
      response.json shouldBe InvitationIdInvalidFormat.toJson

class CancelInvitationV3UnavailableISpec extends BaseISpec:

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> false)

  private given WSClient = app.injector.instanceOf[WSClient]

  private def patch(acceptHeader: Option[String]) =
    given HeaderCarrier = HeaderCarrier()

    new Resource(
      s"/agents/${arn.value}/invitations/${invitationIdITSA.value}",
      port
    ).patchEmpty(acceptHeader.toSeq.map("Accept" -> _))

  "the V3-only PATCH invitation route" should:
    "be unavailable when the V3 switch is off" in:
      val response = patch(Some(ApiVersion.V3AcceptHeader))

      response.status shouldBe 404
      response.json shouldBe StandardNotFound.toJson
      verify(0, putRequestedFor(urlPathMatching(".*/cancel-invitation/.*")))

    "be unavailable without the exact V3 Accept header" in:
      Seq(None, Some("application/vnd.hmrc.2.0+json"), Some("application/json")).foreach: acceptHeader =>
        val response = patch(acceptHeader)

        response.status shouldBe 404
        response.json shouldBe StandardNotFound.toJson

      verify(0, putRequestedFor(urlPathMatching(".*/cancel-invitation/.*")))
