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
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.agentauthorisation.models.{ApiVersion, StandardInternalServerError}
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, Resource}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import scala.concurrent.ExecutionContext.Implicits.global

class GetInvitationsV3ControllerISpec extends BaseISpec:

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> true)

  private lazy val controller = app.injector.instanceOf[GetInvitationsController]
  private given WSClient = app.injector.instanceOf[WSClient]

  private val services = Seq(
    ("HMRC-MTD-IT", "MTD-IT", Some("main"), "income-tax"),
    ("HMRC-MTD-IT-SUPP", "MTD-IT", Some("supporting"), "income-tax"),
    ("HMRC-MTD-VAT", "MTD-VAT", None, "vat"),
    ("HMRC-TERS-ORG", "TRUSTS", None, "trusts-and-estates"),
    ("HMRC-TERSNT-ORG", "TRUSTS", None, "trusts-and-estates"),
    ("PERSONAL-INCOME-RECORD", "IRV", None, "income-record-viewer"),
    ("HMRC-CGT-PD", "CGT-PD", None, "capital-gains-tax-uk-property"),
    ("HMRC-PPT-ORG", "PPT", None, "plastic-packaging-tax"),
    ("HMRC-CBC-ORG", "CBC", None, "country-by-country-reporting"),
    ("HMRC-CBC-NONUK-ORG", "CBC", None, "country-by-country-reporting"),
    ("HMRC-PILLAR2-ORG", "PILLAR2", None, "pillar-2")
  )

  private def request =
    authorisedAsValidAgent(
      FakeRequest(GET, s"/agents/${arn.value}/invitations")
        .withHeaders(ACCEPT -> ApiVersion.V3AcceptHeader, AUTHORIZATION -> "Bearer XYZ"),
      arn.value
    )

  private def acrInvitation(invitationId: String, service: String, status: String): JsObject =
    Json.obj(
      "invitationId" -> invitationId,
      "arn" -> arn.value,
      "service" -> service,
      "suppliedClientId" -> "AB123456A",
      "suppliedClientIdType" -> "ni",
      "clientName" -> "Client Name",
      "agencyName" -> "Agency Name",
      "agencyEmail" -> "agent@example.com",
      "warningEmailSent" -> false,
      "expiredEmailSent" -> false,
      "status" -> status,
      "clientType" -> "personal",
      "expiryDate" -> "2026-10-19",
      "created" -> "2026-09-28T09:00:00Z",
      "lastUpdated" -> "2026-09-29T10:00:00Z"
    )

  private def stubPage(pageNumber: Int, requests: Seq[JsObject], totalResults: Int, status: Int = OK): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-client-relationships/agent/${arn.value}/authorisation-requests"))
        .withQueryParam("pageNumber", equalTo(pageNumber.toString))
        .withQueryParam("pageSize", equalTo("100"))
        .willReturn(
          if status == OK then
            jsonResponse(
              Json.obj(
                "pageNumber" -> pageNumber,
                "requests" -> requests,
                "clientNames" -> Json.arr(),
                "availableFilters" -> Json.arr(),
                "totalResults" -> totalResults
              ).toString,
              OK
            )
          else aResponse().withStatus(status)
        )
    )

  private def stubAgentLink(status: Int = OK): Unit =
    stubFor(
      get(urlEqualTo("/agent-client-relationships/agent/agent-link"))
        .willReturn(
          if status == OK then
            jsonResponse(Json.obj("uid" -> "12345678", "normalizedAgentName" -> "agent-1").toString, OK)
          else aResponse().withStatus(status)
        )
    )

  "GET /agents/:arn/invitations for V3" should:
    "return the V3 schema for every supported ACR service variant" in:
      val invitations = services.zipWithIndex.map:
        case ((acrService, _, _, _), index) =>
          acrInvitation(f"INVITATION$index%02d", acrService, if index == 0 then "Pending" else "Accepted")
      stubPage(1, invitations, invitations.size)
      stubAgentLink()

      val result = controller.getInvitationsApi(arn)(request).futureValue

      status(result) shouldBe OK
      val response = contentAsJson(result).as[JsArray].value
      response should have size services.size

      response.zip(services).zipWithIndex.foreach:
        case ((invitation, (_, publicService, agentType, urlPart)), index) =>
          (invitation \ "href").as[String] shouldBe s"/agents/${arn.value}/invitations/INVITATION${f"$index%02d"}"
          (invitation \ "service").as[String] shouldBe publicService
          (invitation \ "agentType").asOpt[String] shouldBe agentType
          if index == 0 then
            (invitation \ "status").as[String] shouldBe "Pending"
            (invitation \ "expiresOn").as[String] shouldBe "2026-10-19"
            (invitation \ "updated").toOption shouldBe None
            (invitation \ "clientActionUrl").as[String] shouldBe
              s"http://localhost:9435/agent-client-relationships/appoint-someone-to-deal-with-HMRC-for-you/12345678/agent-1/$urlPart"
          else
            (invitation \ "status").as[String] shouldBe "Accepted"
            (invitation \ "updated").as[String] shouldBe "2026-09-29T10:00:00Z"
            (invitation \ "expiresOn").toOption shouldBe None
            (invitation \ "clientActionUrl").toOption shouldBe None

    "return 200 with an empty array and avoid creating an unnecessary agent link" in:
      stubPage(1, Nil, 0)

      val result = controller.getInvitationsApi(arn)(request).futureValue

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.arr()
      verify(0, getRequestedFor(urlEqualTo("/agent-client-relationships/agent/agent-link")))

    "select the V3 list handler at the routed HTTP boundary" in:
      stubPage(1, Nil, 0)
      givenAuthorisedAsValidAgent(arn.value)
      given HeaderCarrier = HeaderCarrier(
        authorization = Some(Authorization("Bearer XYZ"))
      )

      val response = new Resource(s"/agents/${arn.value}/invitations", port)
        .get(Seq(ACCEPT -> ApiVersion.V3AcceptHeader))

      response.status shouldBe OK
      response.json shouldBe Json.arr()

    "retrieve every page of invitation history" in:
      val firstPage = (1 to 100).map(index => acrInvitation(s"INVITATION$index", "HMRC-MTD-VAT", "Accepted"))
      val secondPage = Seq(acrInvitation("INVITATION101", "HMRC-MTD-VAT", "Accepted"))
      stubPage(1, firstPage, 101)
      stubPage(2, secondPage, 101)

      val result = controller.getInvitationsApi(arn)(request).futureValue

      status(result) shouldBe OK
      contentAsJson(result).as[JsArray].value should have size 101
      verify(
        getRequestedFor(urlPathEqualTo(s"/agent-client-relationships/agent/${arn.value}/authorisation-requests"))
          .withQueryParam("pageNumber", equalTo("2"))
      )

    "return an internal error when invitation history cannot be obtained" in:
      stubPage(1, Nil, 0, INTERNAL_SERVER_ERROR)

      val result = controller.getInvitationsApi(arn)(request).futureValue

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe StandardInternalServerError.toJson

    "return an internal error when a later invitation-history page cannot be obtained" in:
      val firstPage = (1 to 100).map(index => acrInvitation(s"INVITATION$index", "HMRC-MTD-VAT", "Accepted"))
      stubPage(1, firstPage, 101)
      stubPage(2, Nil, 101, INTERNAL_SERVER_ERROR)

      val result = controller.getInvitationsApi(arn)(request).futureValue

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe StandardInternalServerError.toJson

    "return an internal error when the agent link cannot be obtained for a pending invitation" in:
      stubPage(1, Seq(acrInvitation("INVITATION01", "HMRC-MTD-IT", "Pending")), 1)
      stubAgentLink(INTERNAL_SERVER_ERROR)

      val result = controller.getInvitationsApi(arn)(request).futureValue

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe StandardInternalServerError.toJson

    "return an internal error for an unsupported stored service" in:
      stubPage(1, Seq(acrInvitation("INVITATION01", "UNKNOWN", "Accepted")), 1)

      val result = controller.getInvitationsApi(arn)(request).futureValue

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe StandardInternalServerError.toJson
