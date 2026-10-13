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
import play.api.test.Helpers.*
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, Resource}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import scala.concurrent.ExecutionContext.Implicits.global

class CheckRelationshipV3ControllerISpec extends BaseISpec:

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> true)

  private given WSClient = app.injector.instanceOf[WSClient]
  private val userId = "agent-user-id"

  private def acrPath(service: String, clientIdType: String, clientId: String): String =
    s"/agent-client-relationships/agent/${arn.value}/service/$service/client/$clientIdType/$clientId"

  private def stubRelationship(
    service: String,
    clientIdType: String,
    clientId: String,
    status: Int,
    userLevel: Boolean
  ): Unit =
    val url = acrPath(service, clientIdType, clientId) + Option.when(userLevel)(s"?userId=$userId").getOrElse("")
    stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(status)))

  private def getRelationship(
    service: String = "MTD-VAT",
    clientId: String = "101747696",
    authenticatedArn: String = arn.value,
    routeArn: String = arn.value,
    acceptHeader: Option[String] = Some(ApiVersion.V3AcceptHeader)
  ) =
    givenAuthorisedAsValidAgentWithUserId(authenticatedArn, userId)
    given HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("Bearer XYZ")))

    new Resource(s"/agents/$routeArn/service/$service/client/$clientId/relationship", port)
      .get(acceptHeader.toSeq.map("Accept" -> _))

  "GET /agents/:arn/service/:service/client/:clientId/relationship for V3" should:
    "return an assigned relationship" in:
      stubRelationship("HMRC-MTD-VAT", "VRN", "101747696", OK, userLevel = true)

      val response = getRelationship()

      response.status shouldBe OK
      response.json shouldBe Json.obj("agentAuthorised" -> true, "userAssigned" -> true)

    "distinguish an unassigned user from an unrelated agency" in:
      stubRelationship("HMRC-MTD-VAT", "VRN", "101747696", NOT_FOUND, userLevel = true)
      stubRelationship("HMRC-MTD-VAT", "VRN", "101747696", OK, userLevel = false)

      val unassigned = getRelationship()

      unassigned.status shouldBe OK
      unassigned.json shouldBe Json.obj("agentAuthorised" -> true, "userAssigned" -> false)

      resetAllRequests()
      stubRelationship("HMRC-MTD-VAT", "VRN", "101747696", NOT_FOUND, userLevel = true)
      stubRelationship("HMRC-MTD-VAT", "VRN", "101747696", NOT_FOUND, userLevel = false)

      val unrelated = getRelationship()

      unrelated.status shouldBe OK
      unrelated.json shouldBe Json.obj("agentAuthorised" -> false)

    "fall back from MTD-IT main and supporting relationships to IR-SA" in:
      Seq("HMRC-MTD-IT", "HMRC-MTD-IT-SUPP").foreach: service =>
        stubRelationship(service, "ni", "AB123456A", NOT_FOUND, userLevel = true)
        stubRelationship(service, "ni", "AB123456A", NOT_FOUND, userLevel = false)
      stubRelationship("IR-SA", "ni", "AB123456A", OK, userLevel = false)

      val response = getRelationship("MTD-IT", "AB123456A")

      response.status shouldBe OK
      response.json shouldBe Json.obj(
        "agentAuthorised" -> true,
        "userAssigned"   -> true,
        "agentType"      -> "mtdSignup"
      )

    "return path validation errors before calling ACR" in:
      val invalidClientId = getRelationship(clientId = "not-a-client-id")
      invalidClientId.status shouldBe BAD_REQUEST
      invalidClientId.json shouldBe ClientIdInvalidFormat.toJson

      val incompatible = getRelationship(service = "MTD-IT", clientId = "101747696")
      incompatible.status shouldBe BAD_REQUEST
      incompatible.json shouldBe ClientIdIncompatibleWithService.toJson

      val unsupportedService = getRelationship(service = "NOT-A-SERVICE")
      unsupportedService.status shouldBe BAD_REQUEST
      unsupportedService.json shouldBe UnsupportedService.toJson

      verify(0, getRequestedFor(urlPathMatching("/agent-client-relationships/agent/.*")))

    "reject a route ARN that differs from the authenticated ARN" in:
      val response = getRelationship(authenticatedArn = arn2.value)

      response.status shouldBe FORBIDDEN
      response.json shouldBe NoPermissionOnAgency.toJson
      verify(0, getRequestedFor(urlPathMatching("/agent-client-relationships/agent/.*")))

    "return a version warning without the exact V3 Accept header" in:
      Seq(None, Some("application/vnd.hmrc.2.0+json"), Some("application/json")).foreach: acceptHeader =>
        val response = getRelationship(acceptHeader = acceptHeader)

        response.status shouldBe NOT_ACCEPTABLE
        response.body shouldBe "invalid client version, this endpoint requires one of [V3]"

      verify(0, getRequestedFor(urlPathMatching("/agent-client-relationships/agent/.*")))

class CheckRelationshipV3UnavailableISpec extends BaseISpec:

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> false)

  private given WSClient = app.injector.instanceOf[WSClient]

  "the V3-only GET relationship route" should:
    "be unavailable when the V3 switch is off" in:
      given HeaderCarrier = HeaderCarrier()
      val response = new Resource(
        s"/agents/${arn.value}/service/MTD-VAT/client/101747696/relationship",
        port
      ).get(Seq("Accept" -> ApiVersion.V3AcceptHeader))

      response.status shouldBe NOT_FOUND
      response.json shouldBe StandardNotFound.toJson
      verifyNoAuthoriseAttempt()
