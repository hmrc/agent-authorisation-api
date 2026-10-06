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
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, Resource}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import scala.concurrent.ExecutionContext.Implicits.global

class GetInvitationByIdV3ControllerISpec extends BaseISpec:

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> true)

  private given WSClient = app.injector.instanceOf[WSClient]

  private val acrPath =
    s"/agent-client-relationships/agent/${arn.value}/authorisation-request-info/${invitationIdITSA.value}"
  private val clientActionUrl =
    "http://localhost:9435/agent-client-relationships/appoint-someone-to-deal-with-HMRC-for-you" +
      "/12345678/agent-1/income-tax"

  private def getInvitation(authenticatedArn: String = arn.value) =
    givenAuthorisedAsValidAgent(authenticatedArn)
    given HeaderCarrier = HeaderCarrier(
      authorization = Some(Authorization("Bearer XYZ"))
    )

    new Resource(s"/agents/${arn.value}/invitations/${invitationIdITSA.value}", port)
      .get(Seq("Accept" -> ApiVersion.V3AcceptHeader))

  private def invitationInfo(status: String, service: String = "HMRC-MTD-IT"): String =
    Json.obj(
      "agentLink" -> Json.obj(
        "uid"                 -> "12345678",
        "normalizedAgentName" -> "agent-1"
      ),
      "authorisationRequest" -> Json.obj(
        "invitationId" -> invitationIdITSA.value,
        "service"      -> service,
        "status"       -> status,
        "expiryDate"   -> "2026-10-19",
        "created"      -> "2026-09-28T09:00:00Z",
        "lastUpdated"  -> "2026-09-29T10:00:00Z"
      )
    ).toString

  private def stubInvitationInfo(status: Int, body: String = ""): Unit =
    stubFor(get(urlEqualTo(acrPath)).willReturn(aResponse().withStatus(status).withBody(body)))

  "GET /agents/:arn/invitations/:invitationId for V3" should:
    "return a pending invitation with its pending-only fields" in:
      stubInvitationInfo(200, invitationInfo("Pending"))

      val response = getInvitation()

      response.status shouldBe 200
      response.json shouldBe Json.obj(
        "invitationId"    -> invitationIdITSA.value,
        "service"         -> "MTD-IT",
        "status"          -> "Pending",
        "created"         -> "2026-09-28T09:00:00Z",
        "expiresOn"       -> "2026-10-19",
        "clientActionUrl" -> clientActionUrl,
        "agentType"       -> "main"
      )
      verify(getRequestedFor(urlEqualTo(acrPath)))

    "return a responded invitation with its updated timestamp" in:
      stubInvitationInfo(200, invitationInfo("Accepted"))

      val response = getInvitation()

      response.status shouldBe 200
      response.json shouldBe Json.obj(
        "invitationId" -> invitationIdITSA.value,
        "service"      -> "MTD-IT",
        "status"       -> "Accepted",
        "created"      -> "2026-09-28T09:00:00Z",
        "updated"      -> "2026-09-29T10:00:00Z",
        "agentType"    -> "main"
      )

    "return INVITATION_NOT_FOUND when ACR cannot find the invitation" in:
      stubInvitationInfo(404)

      val response = getInvitation()

      response.status shouldBe 404
      response.json shouldBe InvitationNotFound.toJson

    "return INTERNAL_SERVER_ERROR for an unexpected ACR response" in:
      stubInvitationInfo(502)

      val response = getInvitation()

      response.status shouldBe 500
      response.json shouldBe StandardInternalServerError.toJson

    "reject an authenticated ARN mismatch before calling ACR" in:
      val response = getInvitation(authenticatedArn = arn2.value)

      response.status shouldBe 403
      response.json shouldBe NoPermissionOnAgency.toJson
      verify(0, getRequestedFor(urlEqualTo(acrPath)))
