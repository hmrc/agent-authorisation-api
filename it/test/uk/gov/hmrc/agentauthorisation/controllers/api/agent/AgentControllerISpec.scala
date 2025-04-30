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

package uk.gov.hmrc.agentauthorisation.controllers.api.agent

import play.api.Configuration
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.agentauthorisation.audit.AgentAuthorisationEvent
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults.{VatRegDateDoesNotMatchResultResult, _}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.Service.{HMRCMTDIT, HMRCMTDITSUPP}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId}
import uk.gov.hmrc.http.SessionKeys

import java.time.LocalDate
import scala.concurrent.Future

class AgentControllerISpec extends BaseISpec {
  lazy val controller: AgentController = app.injector.instanceOf[AgentController]

  lazy val configuration: Configuration = app.injector.instanceOf[Configuration]

  val itsaSupportingAgentEnabled = configuration.get[Boolean]("itsa-supporting-agent.enabled")

  val jsonBodyITSA: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}"""
  )

  val jsonBodyITSASupportingAgentType: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode", "agentType":"supporting"}"""
  )

  val jsonBodyITSAMainAgentType: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode", "agentType":"main"}"""
  )

  val jsonBodyITSAInvalidAgentType: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode", "agentType":"xxxx"}"""
  )

  val jsonBodyVAT: JsValue = Json.parse(
    s"""{"service": ["MTD-VAT"], "clientType":"business", "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "$validVatRegDate"}"""
  )

  val jsonBodyVATAgentType: JsValue = Json.parse(
    s"""{"service": ["MTD-VAT"], "clientType":"business", "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "$validVatRegDate", "agentType":"main"}"""
  )

  def pendingItsaInvitation(service: Service) = PendingInvitation(
    s"/agents/TARN0000001/invitations/ABERULMHCKKW3",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    Arn("TARN0000001"),
    List(service),
    "Pending",
    s"someInvitationUrl/invitations/personal/12345678/agent-1"
  )

  def respondedItsaInvitation(service: Service) = RespondedInvitation(
    s"/agents/TARN0000001/invitations/ABERULMHCKKW3",
    "2017-10-31T23:22:50.971Z",
    "2018-09-11T21:02:00.000Z",
    Arn("TARN0000001"),
    List(service),
    "Accepted"
  )

  val storedVatInvitation = StoredInvitation(
    s"$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent/CZTW1KY6RTAAT",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    "2017-10-31T23:22:50.971Z",
    Arn("TARN0000001"),
    Some("business"),
    Service.Vat,
    "Pending",
    Some("http://localhost:9448/invitations/business/12345678/agent-1")
  )

  val pendingVatInvitation = PendingInvitation(
    s"/agents/TARN0000001/invitations/CZTW1KY6RTAAT",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    Arn("TARN0000001"),
    List(Service.Vat),
    "Pending",
    s"someInvitationUrl/invitations/business/12345678/agent-1"
  )

  val respondedVatInvitation = RespondedInvitation(
    s"/agents/TARN0000001/invitations/CZTW1KY6RTAAT",
    "2017-10-31T23:22:50.971Z",
    "2018-09-11T21:02:00.000Z",
    Arn("TARN0000001"),
    List(Service.Vat),
    "Accepted"
  )

  def gettingPendingInvitations(service: Service) = Seq(
    PendingOrRespondedInvitation(
      Links(s"/agents/${arn.value}/invitations/ABERULMHCKKW3"),
      "2017-10-31T23:22:50.971Z",
      arn,
      List(service),
      "Pending",
      Some("2017-12-18T00:00:00.000"),
      Some("someInvitationUrl/invitations/personal/12345678/agent-1"),
      None
    ),
    PendingOrRespondedInvitation(
      Links(s"/agents/${arn.value}/invitations/CZTW1KY6RTAAT"),
      "2017-10-31T23:22:50.971Z",
      arn,
      List(Service.Vat),
      "Pending",
      Some("2017-12-18T00:00:00.000"),
      Some("someInvitationUrl/invitations/business/12345678/agent-1"),
      None
    )
  )

  implicit val writerPendingOrRespondedInvitation =
    if (itsaSupportingAgentEnabled) PendingOrRespondedInvitation.writesExternalWithAgentType
    else PendingOrRespondedInvitation.writesExternalWithoutAgentType

  implicit val writerRespondedInvitation =
    if (itsaSupportingAgentEnabled) RespondedInvitation.writesExternalWithAgentType
    else RespondedInvitation.writesExternalWithoutAgentType

  implicit val writerPendingInvitation =
    if (itsaSupportingAgentEnabled) PendingInvitation.writesExternalWithAgentType
    else PendingInvitation.writesExternalWithoutAgentType

  "GET /agents/:arn/invitations/:invitationId" when {

    "requesting an ITSA invitation" should {

      val getInvitationItsaApi = controller.getInvitationApi(arn, invitationIdITSA)
      val requestITSA = FakeRequest("GET", s"/agents/${arn.value}/invitations/${invitationIdITSA.value}")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 200 and a json body of a pending invitation" in {

        givenGetITSAInvitationStub(arn, "Pending")

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(pendingItsaInvitation(Service.ItsaMain)).as[JsObject]
      }

      "return 200 and a json body of a pending supporting invitation" in {

        givenGetITSASuppInvitationStub(arn, "Pending")

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(pendingItsaInvitation(Service.ItsaSupp)).as[JsObject]
      }

      "return 200 and a json body of a responded invitation" in {

        givenGetITSAInvitationStub(arn, "Accepted")
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(respondedItsaInvitation(Service.ItsaMain)).as[JsObject]
      }

      "return 200 and a json body of a responded supporting invitation" in {

        givenGetITSASuppInvitationStub(arn, "Accepted")
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(respondedItsaInvitation(Service.ItsaSupp)).as[JsObject]
      }

      "return 403 for Not An Agent" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = getInvitationItsaApi(requestITSA)

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgentResult
      }

      "return 403 for Not an Agent" in {
        givenGetITSAInvitationStub(arn, "Pending")
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
             |    { "authProviders": ["GovernmentGateway"] }
             |  ],
             |  "retrieve":["affinityGroup","allEnrolments"]
             |}
           """.stripMargin,
          s"""
             |{
             |"affinityGroup":"Individual",
             |"allEnrolments": [
             |  { "key":"HMRC-MTD-IT", "identifiers": [
             |    {"key":"MTDITID", "value": "${mtdItId.value}"}
             |  ]}
             |]}
          """.stripMargin
        )

        val result = getInvitationItsaApi(requestITSA.withSession(SessionKeys.authToken -> "Bearer XYZ"))

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgentResult
      }

      "return 403 for Agent Not Subscribed" in {
        givenGetITSAInvitationStub(arn, "Pending")
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
             |    { "authProviders": ["GovernmentGateway"] }
             |  ],
             |  "retrieve":["affinityGroup","allEnrolments"]
             |}
           """.stripMargin,
          s"""
             |{
             |"affinityGroup":"Agent",
             |"allEnrolments": [
             |  { "key":"IR-SA-AGENT", "identifiers": [
             |    {"key":"IRAgentReference", "value": "someIRAR"}
             |  ]}
             |]}
          """.stripMargin
        )

        val result = getInvitationItsaApi(requestITSA.withSession(SessionKeys.authToken -> "Bearer XYZ"))

        status(result) shouldBe 403
        await(result) shouldBe AgentNotSubscribedResult
      }

      "return 403 for No Permission On Agency" in {
        givenGetITSAInvitationStub(arn, "Pending")
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn2.value))
        status(result) shouldBe 403
        await(result) shouldBe NoPermissionOnAgencyResult
      }

      "return 404 for invitation not accessible for this Agent" in {

        givenGetAgentInvitationStubReturns(arn, invitationIdITSA, 403)

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 404
        await(result) shouldBe InvitationNotFoundResult
      }

      "return 404 for Invitation Not Found" in {
        givenInvitationNotFound(arn, invitationIdITSA)
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))
        status(result) shouldBe 404
        await(result) shouldBe InvitationNotFoundResult
      }
    }
    "requesting an VAT invitation" should {
      val getInvitationVatApi = controller.getInvitationApi(arn, invitationIdVAT)
      val requestVAT = FakeRequest("GET", s"/agents/${arn.value}/invitations/${invitationIdVAT.value}")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 200 and a json body of invitation" in {

        givenGetVATInvitationStub(arn, "Pending")
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(pendingVatInvitation).as[JsObject]
      }

      "return 200 and a json body of a responded invitation" in {

        givenGetVATInvitationStub(arn, "Accepted")
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(respondedVatInvitation).as[JsObject]
      }

      "return 403 for Not An Agent" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = getInvitationVatApi(requestVAT)

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgentResult
      }

      "return 403 for Not an Agent" in {
        givenGetVATInvitationStub(arn, "Pending")
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
             |    { "authProviders": ["GovernmentGateway"] }
             |  ],
             |  "retrieve":["affinityGroup","allEnrolments"]
             |}
           """.stripMargin,
          s"""
             |{
             |"affinityGroup":"Individual",
             |"allEnrolments": [
             |  { "key":"HMRC-MTD-IT", "identifiers": [
             |    {"key":"MTDITID", "value": "${mtdItId.value}"}
             |  ]}
             |]}
          """.stripMargin
        )

        val result = getInvitationVatApi(requestVAT.withSession(SessionKeys.authToken -> "Bearer XYZ"))

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgentResult
      }

      "return 403 for Agent Not Subscribed" in {
        givenGetVATInvitationStub(arn, "Pending")
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
             |    { "authProviders": ["GovernmentGateway"] }
             |  ],
             |  "retrieve":["affinityGroup","allEnrolments"]
             |}
           """.stripMargin,
          s"""
             |{
             |"affinityGroup":"Agent",
             |"allEnrolments": [
             |  { "key":"IR-SA-AGENT", "identifiers": [
             |    {"key":"IRAgentReference", "value": "someIRAR"}
             |  ]}
             |]}
          """.stripMargin
        )

        val result = getInvitationVatApi(requestVAT.withSession(SessionKeys.authToken -> "Bearer XYZ"))

        status(result) shouldBe 403
        await(result) shouldBe AgentNotSubscribedResult
      }

      "return 403 for No Permission On Agency" in {
        givenGetVATInvitationStub(arn, "Pending")
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn2.value))
        status(result) shouldBe 403
        await(result) shouldBe NoPermissionOnAgencyResult
      }

      "return 404 for Invitation Not Found" in {
        givenInvitationNotFound(arn, invitationIdVAT)
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))
        status(result) shouldBe 404
        await(result) shouldBe InvitationNotFoundResult
      }

    }

  }

  "DELETE /agents/:arn/invitations/:invitationId" when {
    "cancelling an ITSA invitation" should {

      val cancelInvitationItsaApi = controller.cancelInvitationApi(arn, invitationIdITSA)
      val requestITSA = FakeRequest("DELETE", s"/agents/${arn.value}/invitations/${invitationIdITSA.value}/cancel")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 204 for a successful cancellation" in {
        givenCancelAgentInvitationStub(arn, invitationIdITSA, 204)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))
        status(result) shouldBe 204
        verifyAgentClientInvitationCancelledEvent(arn.value, invitationIdITSA)
      }

      "return 403 INVALID_INVITATION_STATUS when the status of the invitation is not Pending" in {
        givenCancelAgentInvitationStubInvalid(arn, invitationIdITSA)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))
        await(result) shouldBe InvalidInvitationStatusResult
        verifyAgentClientInvitationCancelledEvent(arn.value, invitationIdITSA, Some("INVALID_INVITATION_STATUS"))
      }

      "return 403 NOT_AN_AGENT when the logged in user does not have an affinity group of Agent" in {
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
             |    { "authProviders": ["GovernmentGateway"] }
             |  ],
             |  "retrieve":["affinityGroup","allEnrolments"]
             |}
           """.stripMargin,
          s"""
             |{
             |"affinityGroup":"Individual",
             |"allEnrolments": [
             |  { "key":"HMRC-MTD-IT", "identifiers": [
             |    {"key":"MTDITID", "value": "${mtdItId.value}"}
             |  ]}
             |]}
          """.stripMargin
        )
        val result = cancelInvitationItsaApi(requestITSA)

        await(result) shouldBe NotAnAgentResult
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisedCancelledViaApi)

      }

      "return 403 AGENT_NOT_SUBSCRIBED when the logged in user does not have HMRC_AS_AGENT enrolment" in {
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
             |    { "authProviders": ["GovernmentGateway"] }
             |  ],
             |  "retrieve":["affinityGroup","allEnrolments"]
             |}
           """.stripMargin,
          s"""
             |{
             |"affinityGroup":"Agent",
             |"allEnrolments": [
             |  { "key":"IR-SA-AGENT", "identifiers": [
             |    {"key":"IRAgentReference", "value": "someIRAR"}
             |  ]}
             |]}
          """.stripMargin
        )
        val result = cancelInvitationItsaApi(requestITSA)

        await(result) shouldBe AgentNotSubscribedResult
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisedCancelledViaApi)
      }

      "return 403 NO_PERMISSION_ON_AGENCY when the arn given does not match the logged in user" in {
        givenCancelAgentInvitationStub(arn, invitationIdITSA, 204)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn2.value))
        await(result) shouldBe NoPermissionOnAgencyResult
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisedCancelledViaApi)
      }
    }

    "cancelling a VAT invitation" should {

      val cancelInvitationVatApi = controller.cancelInvitationApi(arn, invitationIdVAT)
      val requestVAT = FakeRequest("DELETE", s"/agents/${arn.value}/invitations/${invitationIdITSA.value}/cancel")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 204 for a successful cancellation" in {
        givenCancelAgentInvitationStub(arn, invitationIdVAT, 204)
        val result = cancelInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))
        status(result) shouldBe 204
        verifyAgentClientInvitationCancelledEvent(arn.value, invitationIdVAT)
      }
    }
  }

  "POST /agents/:arn/relationships" when {

    "getting the status of an ITSA relationship" should {
      val checkRelationshipApi = controller.checkRelationshipApi(arn)
      val request = FakeRequest("POST", s"/agents/$arn/relationships")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 204 when the relationship is active for ITSA" in {
        getStatusRelationshipItsa(arn.value, validNino, 200, HMRCMTDIT)
        givenMatchingClientIdAndPostcode(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))
        status(result) shouldBe 204
        verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
      }

      "return 204 when the relationship is active for ITSA supporting" in {
        getStatusRelationshipItsa(arn.value, validNino, 200, HMRCMTDITSUPP)
        givenMatchingClientIdAndPostcode(validNino, validPostcode)
        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSASupportingAgentType), arn.value))
        if (itsaSupportingAgentEnabled) {
          status(result) shouldBe 204
          verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
          verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
        } else {
          status(result) shouldBe 400
          await(result) shouldBe InvalidPayloadResult
          verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
          verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
          verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
        }
      }

      "return 204 when the relationship is active for VAT" in {
        getStatusRelationshipVat(arn.value, validVrn, 200)
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))
        status(result) shouldBe 204
      }

      "return 404 when the relationship is not found for ITSA" in {
        getStatusRelationshipItsa(arn.value, validNino, 404, HMRCMTDIT)
        givenMatchingClientIdAndPostcode(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))
        status(result) shouldBe 404
        verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
      }

      "return 404 when the relationship is not found for VAT" in {
        getStatusRelationshipVat(arn.value, validVrn, 404)
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))
        status(result) shouldBe 404
      }

      "return 400 SERVICE_NOT_SUPPORTED when the service is not supported" in {
        val jsonBodyInvalidService = Json.parse(
          s"""{"service": ["foo"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe UnsupportedServiceResult
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      }

      "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for ITSA" in {
        val jsonBodyInvalidClientId = Json.parse(
          s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "foo", "knownFact": "$validPostcode"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdInvalidFormatResult
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)

      }

      "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for VAT" in {
        val jsonBodyInvalidClientId = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientType":"business", "clientIdType": "vrn", "clientId": "foo", "knownFact": "$validVatRegDate"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdInvalidFormatResult
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      }

      "return 400 POSTCODE_FORMAT_INVALID when the postcode has an invalid format" in {
        val jsonBodyInvalidPostcode = Json.parse(
          s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidPostcode), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe PostcodeFormatInvalidResult
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)

      }

      "return 400 VAT_REG_DATE_FORMAT_INVALID when the VAT registration date has an invalid format" in {
        val jsonBodyInvalidVatRegDate = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientType":"business", "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidVatRegDate), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe VatRegDateFormatInvalidResult
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)

      }

      "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for ITSA" in {
        val jsonBodyClientIdNotMatchService = Json.parse(
          s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validVrn.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdDoesNotMatchServiceResult
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)

      }

      "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for VAT" in {
        val jsonBodyClientIdNotMatchService = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientType":"business", "clientIdType": "vrn", "clientId": "${validNino.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdDoesNotMatchServiceResult
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      }

      "return 403 CLIENT_REGISTRATION_NOT_FOUND when the postcode returns nothing" in {
        givenNotEnrolledClientITSA(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe ClientRegistrationNotFoundResult
      }

      "return 403 CLIENT_REGISTRATION_NOT_FOUND when the VAT registration date returns nothing" in {
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 404)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe ClientRegistrationNotFoundResult
      }

      "return 403 POSTCODE_DOES_NOT_MATCH when the postcode and clientId do not match" in {
        givenNonMatchingClientIdAndPostcode(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe PostcodeDoesNotMatchResult
      }

      "return 403 VAT_REG_DATE_DOES_NOT_MATCH when the VAT registration date and clientId do not match" in {
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 403)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe VatRegDateDoesNotMatchResultResult
      }

      "return 403 NOT_AN_AGENT when the logged in user is not have an HMRC-AS-AGENT enrolment" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = checkRelationshipApi(request.withJsonBody(jsonBodyITSA))

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgentResult
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      }

      "return 403 VAT_CLIENT_INSOLVENT when the when the VAT customer is insolvent" in {
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 403, true)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe VatClientInsolventResult
      }

    }

    "GET /agents/:arn/invitations/" when {

      "requesting a sequence of ITSA and VAT invitations" should {

        val getInvitations = controller.getInvitationsApi(arn)
        val request = FakeRequest("GET", s"/agents/${arn.value}/invitations")
          .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

        "return 200 and a json body of a pending invitation filtering out PIR and TERS invitations" in {
          givenInvitationsServiceReturns(arn, Seq(itsa(arn), vat(arn)))
          val result = getInvitations(authorisedAsValidAgent(request, arn.value))

          status(result) shouldBe 200
          Helpers.contentAsJson(result) shouldBe toJson(gettingPendingInvitations(Service.ItsaMain))
        }

        "return 200 and a json body of a responded invitation IRV and TERS invitations" in {
          givenInvitationsServiceReturns(arn, Seq(irv(arn), ters(arn)))

          intercept[RuntimeException] {
            await(getInvitations(authorisedAsValidAgent(request, arn.value)))
          }.getMessage shouldBe "Unexpected Service has been passed through: PERSONAL-INCOME-RECORD"
        }

        "return 204 if there are no invitations for the agent" in {
          givenAllInvitationsEmptyStub(arn)
          val result = getInvitations(authorisedAsValidAgent(request, arn.value))

          status(result) shouldBe 204
        }
      }
    }
  }

  def verifyAgentClientInvitationSubmittedEvent(
    arn: String,
    clientId: String,
    clientIdType: String,
    result: String,
    service: String,
    failure: Option[String] = None
  ): Unit =
    verifyAuditRequestSent(
      1,
      AgentAuthorisationEvent.agentAuthorisationCreatedViaApi,
      detail = Map(
        "factCheck"            -> result,
        "agentReferenceNumber" -> arn,
        "clientIdType"         -> clientIdType,
        "clientId"             -> clientId,
        "service"              -> service
      )
        .filter(_._2.nonEmpty) ++ failure.map(e => Seq("failureDescription" -> e)).getOrElse(Seq.empty),
      tags = Map("transactionName" -> "agent-created-invitation-via-api")
    )

  def verifyAgentClientInvitationCancelledEvent(
    arn: String,
    invitationId: InvitationId,
    failure: Option[String] = None
  ): Unit =
    verifyAuditRequestSent(
      1,
      AgentAuthorisationEvent.agentAuthorisedCancelledViaApi,
      detail = Map("invitationId" -> invitationId.value, "agentReferenceNumber" -> arn)
        .filter(_._2.nonEmpty) ++ failure.map(e => Seq("failureDescription" -> e)).getOrElse(Seq.empty),
      tags = Map("transactionName" -> "agent-cancelled-invitation-via-api")
    )
}
