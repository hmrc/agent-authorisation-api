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
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, Http}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class CreateInvitationV3ControllerISpec extends BaseISpec:

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> true)

  private lazy val controller = app.injector.instanceOf[CreateInvitationController]
  private given WSClient = app.injector.instanceOf[WSClient]
  private val acceptV3 = "application/vnd.hmrc.3.0+json"

  private case class Scenario(
    publicService: String,
    clientId: String,
    knownFact: Option[String],
    knownFactType: Option[String],
    detailsService: String,
    createService: String,
    clientIdType: String,
    agentType: Option[String] = None,
    overseas: Option[Boolean] = None,
    clientType: Option[String] = None
  )

  private val scenarios = Seq(
    Scenario("MTD-IT", "AB123456A", Some("AA1 1AA"), Some("PostalCode"), "HMRC-MTD-IT", "HMRC-MTD-IT", "ni"),
    Scenario("MTD-IT", "AB123456A", Some("FR"), Some("CountryCode"), "HMRC-MTD-IT", "HMRC-MTD-IT-SUPP", "ni", Some("supporting"), Some(true)),
    Scenario("MTD-VAT", "101747696", Some("2020-01-01"), Some("Date"), "HMRC-MTD-VAT", "HMRC-MTD-VAT", "vrn"),
    Scenario("TRUSTS", "1234567890", None, None, "HMRC-TERS-ORG", "HMRC-TERS-ORG", "utr"),
    Scenario("TRUSTS", "XXTRUST12345678", None, None, "HMRC-TERSNT-ORG", "HMRC-TERSNT-ORG", "urn"),
    Scenario("IRV", "AB123456A", Some("1990-01-01"), Some("Date"), "PERSONAL-INCOME-RECORD", "PERSONAL-INCOME-RECORD", "ni"),
    Scenario("CGT-PD", "XACGTP123456789", Some("AA1 1AA"), Some("PostalCode"), "HMRC-CGT-PD", "HMRC-CGT-PD", "CGTPDRef", clientType = Some("personal")),
    Scenario("CGT-PD", "XACGTP987654321", Some("AA1 1AA"), Some("PostalCode"), "HMRC-CGT-PD", "HMRC-CGT-PD", "CGTPDRef", clientType = Some("trust")),
    Scenario("PPT", "XAPPT0001234567", Some("2020-01-01"), Some("Date"), "HMRC-PPT-ORG", "HMRC-PPT-ORG", "EtmpRegistrationNumber"),
    Scenario("CBC", "XACBC1234567890", Some("client@example.com"), Some("Email"), "HMRC-CBC-ORG", "HMRC-CBC-NONUK-ORG", "cbcId", overseas = Some(true)),
    Scenario("PILLAR2", "XAPLR1234567890", Some("2020-01-01"), Some("Date"), "HMRC-PILLAR2-ORG", "HMRC-PILLAR2-ORG", "PLRID")
  )

  private def requestBody(scenario: Scenario): JsObject =
    Json.obj(
      "service" -> scenario.publicService,
      "clientId" -> scenario.clientId,
      "knownFact" -> scenario.knownFact,
      "agentType" -> scenario.agentType
    )

  private def request(body: JsObject) =
    authorisedAsValidAgent(
      FakeRequest(POST, s"/agents/${arn.value}/invitations")
        .withHeaders(ACCEPT -> acceptV3, AUTHORIZATION -> "Bearer XYZ")
        .withJsonBody(body),
      arn.value
    )

  private def routedV3HeaderCarrier: HeaderCarrier =
    HeaderCarrier(
      authorization = Some(Authorization("Bearer XYZ"))
    )

  private def postV3(body: String)(using HeaderCarrier) =
    Http.post(
      s"http://localhost:$port/agents/${arn.value}/invitations",
      body,
      Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON, HeaderNames.ACCEPT -> acceptV3)
    )

  private def stubDetails(
    scenario: Scenario,
    status: Option[String] = None,
    knownFacts: Seq[String],
    pending: Boolean = false,
    existing: Option[String] = None
  ): Unit =
    stubFor(
      get(urlEqualTo(s"/agent-client-relationships/client/${scenario.detailsService}/details/${scenario.clientId}"))
        .willReturn(
          jsonResponse(
            Json.obj(
              "name" -> "Client Name",
              "status" -> status,
              "isOverseas" -> scenario.overseas,
              "knownFacts" -> knownFacts,
              "knownFactType" -> scenario.knownFactType,
              "hasPendingInvitation" -> pending,
              "hasExistingRelationshipFor" -> existing,
              "clientType" -> scenario.clientType
            ).toString,
            OK
          )
        )
    )

  private def stubCreate(scenario: Scenario, responseStatus: Int = CREATED, code: Option[String] = None): Unit =
    val response = code.fold(Json.obj("invitationId" -> "ABERULMHCKKW3"))(value => Json.obj("code" -> value))
    val expectedRequest = Json.obj(
      "clientId" -> scenario.clientId,
      "suppliedClientIdType" -> scenario.clientIdType,
      "clientName" -> "Client Name",
      "service" -> scenario.createService
    ) ++ scenario.clientType.fold(Json.obj())(value => Json.obj("clientType" -> value))
    stubFor(
      post(urlEqualTo(s"/agent-client-relationships/agent/${arn.value}/authorisation-request"))
        .withRequestBody(
          equalToJson(expectedRequest.toString)
        )
        .willReturn(jsonResponse(response.toString, responseStatus))
    )

  "POST /agents/:arn/invitations for V3" should:
    "create an invitation using the correct ACR contracts for every service variant" in:
      scenarios.foreach: scenario =>
        stubDetails(scenario, knownFacts = scenario.knownFact.toSeq)
        stubCreate(scenario)

        val result = controller.createInvitation(arn)(request(requestBody(scenario))).futureValue

        status(result) shouldBe CREATED
        contentAsJson(result) shouldBe Json.obj("invitationId" -> "ABERULMHCKKW3")

    "return CLIENT_REGISTRATION_NOT_FOUND when ACR has no client details" in:
      val scenario = scenarios.head
      stubFor(
        get(urlEqualTo(s"/agent-client-relationships/client/${scenario.detailsService}/details/${scenario.clientId}"))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      val result = controller.createInvitation(arn)(request(requestBody(scenario))).futureValue

      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe ClientRegistrationNotFound.toJson

    "return KNOWN_FACT_DOES_NOT_MATCH without creating an invitation" in:
      val scenario = scenarios.head
      stubDetails(scenario, knownFacts = Seq("BB2 2BB"))

      val result = controller.createInvitation(arn)(request(requestBody(scenario))).futureValue

      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe KnownFactDoesNotMatch.toJson
      verify(0, postRequestedFor(urlPathMatching(".*/authorisation-request")))

    "return CLIENT_INSOLVENT when the client registration is not active" in:
      val scenario = scenarios(2)
      stubDetails(scenario, status = Some("Insolvent"), knownFacts = scenario.knownFact.toSeq)

      val result = controller.createInvitation(arn)(request(requestBody(scenario))).futureValue

      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe ClientInsolvent.toJson

    "return duplicate as 409 when details or final creation detects it" in:
      val scenario = scenarios.head
      stubDetails(scenario, knownFacts = scenario.knownFact.toSeq, pending = true)
      val pendingResult = controller.createInvitation(arn)(request(requestBody(scenario))).futureValue

      status(pendingResult) shouldBe CONFLICT
      contentAsJson(pendingResult) shouldBe DuplicateAuthorisationRequestV3.toJson

      stubDetails(scenario, knownFacts = scenario.knownFact.toSeq)
      stubCreate(scenario, FORBIDDEN, Some("DUPLICATE_AUTHORISATION_REQUEST"))
      val createResult = controller.createInvitation(arn)(request(requestBody(scenario))).futureValue

      status(createResult) shouldBe CONFLICT
      contentAsJson(createResult) shouldBe DuplicateAuthorisationRequestV3.toJson

    "return ALREADY_AUTHORISED when details report an existing relationship" in:
      val scenario = scenarios.head
      stubDetails(scenario, knownFacts = scenario.knownFact.toSeq, existing = Some("HMRC-MTD-IT"))

      val result = controller.createInvitation(arn)(request(requestBody(scenario))).futureValue

      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe AlreadyAuthorised.toJson

    "return V3 payload validation errors before calling ACR" in:
      val result = controller.createInvitation(arn)(
        request(Json.obj("service" -> "MTD-IT", "clientId" -> "101747696", "knownFact" -> "AA1 1AA"))
      ).futureValue

      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe ClientIdNotCompatibleWithService.toJson
      verify(0, getRequestedFor(urlPathMatching("/agent-client-relationships/client/.*")))

    "select the V3 handler at the routed HTTP boundary" in:
      val scenario = scenarios.head
      givenAuthorisedAsValidAgent(arn.value)
      stubDetails(scenario, knownFacts = scenario.knownFact.toSeq)
      stubCreate(scenario)
      given HeaderCarrier = routedV3HeaderCarrier

      val response = postV3(requestBody(scenario).toString)

      response.status shouldBe CREATED
      response.json shouldBe Json.obj("invitationId" -> "ABERULMHCKKW3")

    "return INVALID_PAYLOAD for malformed JSON at the routed HTTP boundary" in:
      givenAuthorisedAsValidAgent(arn.value)
      given HeaderCarrier = routedV3HeaderCarrier

      val response = postV3("{")

      response.status shouldBe BAD_REQUEST
      response.json shouldBe InvalidPayload.toJson

    "return ALREADY_BEING_PROCESSED when the invitation lock is held" in:
      val scenario = scenarios(2)
      val lockRepository = app.injector.instanceOf[MongoLockRepository]
      lockRepository.takeLock(
        lockId = s"create-invitation-${arn.value}-${scenario.detailsService}-${scenario.clientId}",
        owner = "v3-integration-test",
        ttl = 10.seconds
      ).futureValue

      val result = controller.createInvitation(arn)(request(requestBody(scenario))).futureValue

      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe LockedRequest.toJson
