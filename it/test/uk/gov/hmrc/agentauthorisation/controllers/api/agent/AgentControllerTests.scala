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
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults.{VatRegDateDoesNotMatch, _}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.Service.{HMRCMTDIT, HMRCMTDITSUPP}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId}
import uk.gov.hmrc.http.SessionKeys

import java.time.LocalDate
import scala.concurrent.Future

trait AgentControllerTests extends BaseISpec {

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

  "POST /agents/:arn/invitations" should {

    val request = FakeRequest("POST", s"/agents/${arn.value}/invitations")
      .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")
    val createInvitation = controller.createInvitationApi(arn)

    "return 204 when invitation is successfully created for ITSA without an agentType" in {
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDIT)
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      getStatusRelationshipItsa(arn.value, validNino, 404, HMRCMTDIT)
      givenPlatformAnalyticsEventWasSent()
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      createInvitationStub(
        arn,
        validNino.value,
        invitationIdITSA,
        validNino.value,
        "ni",
        "personal",
        "HMRC-MTD-IT",
        "MTDITID",
        validPostcode
      )

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 204
      header("Location", result) shouldBe Some("/agents/TARN0000001/invitations/ABERULMHCKKW3")
      verifyAgentClientInvitationSubmittedEvent(arn.value, validNino.value, "ni", "Success", "HMRC-MTD-IT", None)
      verifyPlatformAnalyticsEventWasSent("create-authorisation-request", Some("HMRC-MTD-IT"))
      verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)

    }

    "return 204 when invitation is successfully created for ITSA with a supporting agent" in {
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDIT)
      getStatusRelationshipItsa(arn.value, validNino, 404, HMRCMTDITSUPP)
      givenPlatformAnalyticsEventWasSent()
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      createInvitationStub(
        arn,
        validNino.value,
        invitationIdITSA,
        suppliedClientId = validNino.value,
        suppliedClientType = "ni",
        clientType = "personal",
        service = HMRCMTDITSUPP,
        serviceIdentifier = "MTDITID",
        knownFact = validPostcode
      )

      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSASupportingAgentType), arn.value))

      if (itsaSupportingAgentEnabled) {
        status(result) shouldBe 204
        header("Location", result) shouldBe Some("/agents/TARN0000001/invitations/ABERULMHCKKW3")
        verifyAgentClientInvitationSubmittedEvent(arn.value, validNino.value, "ni", "Success", "HMRC-MTD-IT-SUPP", None)
        verifyPlatformAnalyticsEventWasSent("create-authorisation-request", Some("HMRC-MTD-IT-SUPP"))
        verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      } else {
        status(result) shouldBe 400
        await(result) shouldBe InvalidPayload
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      }
    }

    "return 204 when invitation is successfully created for ITSA with a main agent" in {
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDIT)
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      getStatusRelationshipItsa(arn.value, validNino, 404, HMRCMTDIT)
      givenPlatformAnalyticsEventWasSent()
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      createInvitationStub(
        arn,
        validNino.value,
        invitationIdITSA,
        validNino.value,
        "ni",
        "personal",
        HMRCMTDIT,
        "MTDITID",
        validPostcode
      )

      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSAMainAgentType), arn.value))

      if (itsaSupportingAgentEnabled) {
        status(result) shouldBe 204
        header("Location", result) shouldBe Some("/agents/TARN0000001/invitations/ABERULMHCKKW3")
        verifyAgentClientInvitationSubmittedEvent(arn.value, validNino.value, "ni", "Success", "HMRC-MTD-IT", None)
        verifyPlatformAnalyticsEventWasSent("create-authorisation-request", Some("HMRC-MTD-IT"))
        verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
      } else {
        status(result) shouldBe 400
        await(result) shouldBe InvalidPayload
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      }
    }

    "return 403 when request to create a main agent and there a pending invitation for main agent" in {
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      givenPendingInvitationsExist(arn, validNino, Service.ItsaMain)
      givenPendingInvitationsExist(arn, validNino, Service.ItsaSupp)

      givenMatchingClientIdAndPostcode(validNino, validPostcode)

      val locationLink: String = "/agents/TARN0000001/invitations/foo"
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe DuplicateAuthorisationRequest.withHeaders(LOCATION -> locationLink)
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 403 when request to create a main agent and there a pending invitation for supporting agent" in {
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDIT)
      givenPendingInvitationsExist(arn, validNino, Service.ItsaSupp)
      givenPendingInvitationsExist(arn, validNino, Service.ItsaMain)
      getStatusRelationshipItsa(arn.value, validNino, 204, HMRCMTDIT)

      givenMatchingClientIdAndPostcode(validNino, validPostcode)

      val locationLink: String = "/agents/TARN0000001/invitations/foo"
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe DuplicateAuthorisationRequest.withHeaders(LOCATION -> locationLink)
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)

    }

    "return 403 when request to create a supporting agent and there a pending invitation for main agent" in {
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      givenPendingInvitationsExist(arn, validNino, Service.ItsaMain)
      givenPendingInvitationsExist(arn, validNino, Service.ItsaSupp)
      getStatusRelationshipItsa(arn.value, validNino, 204, HMRCMTDITSUPP)

      givenMatchingClientIdAndPostcode(validNino, validPostcode)

      val locationLink: String = "/agents/TARN0000001/invitations/foo"
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSASupportingAgentType), arn.value))

      if (itsaSupportingAgentEnabled) {
        status(result) shouldBe 403
        await(result) shouldBe DuplicateAuthorisationRequest.withHeaders(LOCATION -> locationLink)
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      } else {
        status(result) shouldBe 400
        await(result) shouldBe InvalidPayload
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      }
    }

    "return 403 when request to create a supporting agent and there a pending invitation for supporting agent" in {
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDIT)
      givenPendingInvitationsExist(arn, validNino, Service.ItsaSupp)
      givenPendingInvitationsExist(arn, validNino, Service.ItsaMain)

      givenMatchingClientIdAndPostcode(validNino, validPostcode)

      val locationLink: String = "/agents/TARN0000001/invitations/foo"
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSASupportingAgentType), arn.value))

      if (itsaSupportingAgentEnabled) {
        status(result) shouldBe 403
        await(result) shouldBe DuplicateAuthorisationRequest.withHeaders(LOCATION -> locationLink)
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      } else {
        status(result) shouldBe 400
        await(result) shouldBe InvalidPayload
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      }
    }

    "return 403 when request to create ITSA main agent invitation and there an active main relationship" in {
      givenOnlyAcceptedInvitationsExistForClient(arn, validNino, HMRCMTDIT, "Accepted")
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      getStatusRelationshipItsa(arn.value, validNino, 200, HMRCMTDIT)

      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      val locationLink: String = "/agents/TARN0000001/invitations/foo"
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe AlreadyAuthorised.withHeaders(LOCATION -> locationLink)
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)

    }

    "return 403 when request to create ITSA  supporting agent invitation and there an active supporting relationship" in {
      givenOnlyAcceptedInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP, "Accepted")
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDIT)
      getStatusRelationshipItsa(arn.value, validNino, 200, HMRCMTDITSUPP)

      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      val locationLink: String = "/agents/TARN0000001/invitations/foo"
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSASupportingAgentType), arn.value))

      if (itsaSupportingAgentEnabled) {
        status(result) shouldBe 403
        await(result) shouldBe AlreadyAuthorised.withHeaders(LOCATION -> locationLink)
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
        verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      } else {
        status(result) shouldBe 400
        await(result) shouldBe InvalidPayload
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      }
    }

    "return 403 when request to create a main agent and there an active relationship and no invitation" in {
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDIT)
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      getStatusRelationshipItsa(arn.value, validNino, 200, HMRCMTDIT)

      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe AlreadyAuthorised
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)

    }

    "return 403 when request to create a supporting agent and there an active relationship and no invitation" in {
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDIT)
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      getStatusRelationshipItsa(arn.value, validNino, 200, HMRCMTDITSUPP)

      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSASupportingAgentType), arn.value))

      if (itsaSupportingAgentEnabled) {
        status(result) shouldBe 403
        await(result) shouldBe AlreadyAuthorised
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
        verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      } else {
        status(result) shouldBe 400
        await(result) shouldBe InvalidPayload
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      }
    }

    "return 403 when request to create ITSA main agent invitation and there an active Alt main invitation Partialauth" in {
      givenOnlyAcceptedInvitationsExistForClient(arn, validNino, HMRCMTDIT, "Partialauth")
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      getStatusRelationshipItsa(arn.value, validNino, 404, HMRCMTDIT)

      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      val locationLink: String = "/agents/TARN0000001/invitations/foo"
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe AlreadyAuthorised.withHeaders(LOCATION -> locationLink)
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)

    }

    "return 403 when request to create ITSA supporting agent invitation and there an active Alt supporting invitation Partialauth" in {
      givenOnlyAcceptedInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP, "Partialauth")
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDIT)
      getStatusRelationshipItsa(arn.value, validNino, 404, HMRCMTDITSUPP)

      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      val locationLink: String = "/agents/TARN0000001/invitations/foo"
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSASupportingAgentType), arn.value))
      if (itsaSupportingAgentEnabled) {
        status(result) shouldBe 403
        await(result) shouldBe AlreadyAuthorised.withHeaders(LOCATION -> locationLink)
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
        verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      } else {
        status(result) shouldBe 400
        await(result) shouldBe InvalidPayload
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      }
    }

    "return 400 when invitation is successfully created for ITSA with invalid agent" in {
      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSAInvalidAgentType), arn.value))

      if (itsaSupportingAgentEnabled) {
        status(result) shouldBe 400
        await(result) shouldBe UnsupportedAgentType
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      } else {
        status(result) shouldBe 400
        await(result) shouldBe InvalidPayload
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      }
    }

    "return 204 when invitation is successfully created for VAT" in {
      givenNoInvitationsExistForClient(arn, validVrn, "HMRC-MTD-VAT")
      getStatusRelationshipVat(arn.value, validVrn, 404)
      givenPlatformAnalyticsEventWasSent()
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
      createInvitationStub(
        arn,
        validVrn.value,
        invitationIdVAT,
        validVrn.value,
        "vrn",
        "business",
        "HMRC-MTD-VAT",
        "VRN",
        validVatRegDate
      )

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      status(result) shouldBe 204
      header("Location", result) shouldBe Some("/agents/TARN0000001/invitations/CZTW1KY6RTAAT")
      verifyAgentClientInvitationSubmittedEvent(arn.value, validVrn.value, "vrn", "Success", "HMRC-MTD-VAT", None)
      verifyPlatformAnalyticsEventWasSent("create-authorisation-request", Some("HMRC-MTD-VAT"))
    }

    "return 400 when invitation provided an agentType however is not supported for VAT" in {
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVATAgentType), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe InvalidPayload
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 400 SERVICE_NOT_SUPPORTED when the service is not supported" in {
      val jsonBodyInvalidService = Json.parse(
        s"""{"service": ["foo"], "clientType": "personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}"""
      )

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe UnsupportedService
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for ITSA" in {
      val jsonBodyInvalidClientId = Json.parse(
        s"""{"service": ["MTD-IT"], "clientType": "personal", "clientIdType": "ni", "clientId": "foo", "knownFact": "$validPostcode"}"""
      )

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe ClientIdInvalidFormat
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for VAT" in {
      val jsonBodyInvalidClientId = Json.parse(
        s"""{"service": ["MTD-VAT"], "clientType": "business", "clientIdType": "vrn", "clientId": "foo", "knownFact": "$validVatRegDate"}"""
      )

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe ClientIdInvalidFormat
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 400 POSTCODE_FORMAT_INVALID when the postcode has an invalid format" in {
      val jsonBodyInvalidPostcode = Json.parse(
        s"""{"service": ["MTD-IT"], "clientType": "personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "foo"}"""
      )

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidPostcode), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe PostcodeFormatInvalid
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 400 VAT_REG_DATE_FORMAT_INVALID when the VAT registration date has an invalid format" in {
      val jsonBodyInvalidVatRegDate = Json.parse(
        s"""{"service": ["MTD-VAT"], "clientType": "business", "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "foo"}"""
      )

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidVatRegDate), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe VatRegDateFormatInvalid
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for ITSA" in {
      val jsonBodyClientIdNotMatchService = Json.parse(
        s"""{"service": ["MTD-IT"], "clientType": "personal", "clientIdType": "ni", "clientId": "${validVrn.value}", "knownFact": "foo"}"""
      )

      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe ClientIdDoesNotMatchService
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for VAT" in {
      val jsonBodyClientIdNotMatchService = Json.parse(
        s"""{"service": ["MTD-VAT"], "clientType": "business", "clientIdType": "vrn", "clientId": "${validNino.value}", "knownFact": "foo"}"""
      )

      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe ClientIdDoesNotMatchService
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 403 CLIENT_REGISTRATION_NOT_FOUND when the postcode returns nothing" in {
      givenNotEnrolledClientITSA(validNino, validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe ClientRegistrationNotFound
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validNino.value,
        "ni",
        "Fail",
        "HMRC-MTD-IT",
        Some("CLIENT_REGISTRATION_NOT_FOUND")
      )
    }

    "return 403 CLIENT_REGISTRATION_NOT_FOUND when the VAT registration date returns nothing" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 404)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe ClientRegistrationNotFound
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validVrn.value,
        "vrn",
        "Fail",
        "HMRC-MTD-VAT",
        Some("CLIENT_REGISTRATION_NOT_FOUND")
      )
    }

    "return 403 POSTCODE_DOES_NOT_MATCH when the postcode and clientId do not match" in {
      givenNonMatchingClientIdAndPostcode(validNino, validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe PostcodeDoesNotMatch
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validNino.value,
        "ni",
        "Fail",
        "HMRC-MTD-IT",
        Some("POSTCODE_DOES_NOT_MATCH")
      )
    }

    "return 403 VAT_REG_DATE_DOES_NOT_MATCH when the VAT registration date and clientId do not match" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 403)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe VatRegDateDoesNotMatch
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validVrn.value,
        "vrn",
        "Fail",
        "HMRC-MTD-VAT",
        Some("VAT_REGISTRATION_DATE_DOES_NOT_MATCH")
      )
    }

    "return 403 NOT_AN_AGENT when the logged in user does not have an HMRC-AS-AGENT enrolment" in {
      givenUnauthorisedForInsufficientEnrolments()
      val result = createInvitation(request.withJsonBody(jsonBodyITSA))

      status(result) shouldBe 403
      await(result) shouldBe NotAnAgent
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 403 NOT_PERMISSION_ON_AGENCY when the logged in user does not have an HMRC-AS-AGENT enrolment" in {
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn2.value))

      status(result) shouldBe 403
      await(result) shouldBe NoPermissionOnAgency
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 403 DUPLICATE_AUTHORISATION_REQUEST when there is already a pending invitation" in {
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      givenOnlyPendingInvitationsExistForClient(arn, validNino, "HMRC-MTD-IT")
      givenMatchingClientIdAndPostcode(validNino, validPostcode)

      val locationLink: String = "/agents/TARN0000001/invitations/foo"
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe DuplicateAuthorisationRequest.withHeaders(LOCATION -> locationLink)
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
    }

    "return 403 ALREADY_AUTHORISED when there is already an active relationship" in {
      givenOnlyAcceptedInvitationsExistForClient(arn, validNino, HMRCMTDIT, "Accepted")
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      getStatusRelationshipItsa(arn.value, validNino, 200, HMRCMTDIT)
      givenMatchingClientIdAndPostcode(validNino, validPostcode)

      val locationLink: String = "/agents/TARN0000001/invitations/foo"
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe AlreadyAuthorised.withHeaders(LOCATION -> locationLink)
      verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
    }

    "return 403 VAT_CLIENT_INSOLVENT when the VAT customer is insolvent" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 403, true)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe VatClientInsolvent
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validVrn.value,
        "vrn",
        "Fail",
        "HMRC-MTD-VAT",
        Some("VAT_RECORD_CLIENT_INSOLVENT_TRUE")
      )
    }

    "return 500 Internal Server Error when the VAT record is being migrated to ETMP" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 423, false)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      status(result) shouldBe 500
      await(result) shouldBe InternalServerError
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validVrn.value,
        "vrn",
        "Fail",
        "HMRC-MTD-VAT",
        Some("MIGRATION_IN_PROGRESS")
      )
    }

    "return a future failed when the invitation creation failed for ITSA" in {
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDIT)
      givenNoInvitationsExistForClient(arn, validNino, HMRCMTDITSUPP)
      getStatusRelationshipItsa(arn.value, validNino, 404, HMRCMTDIT)
      failedCreateInvitation(arn)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      an[Exception] shouldBe thrownBy {
        await(result)
      }
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validNino.value,
        clientIdType = "ni",
        result = "Fail",
        service = "HMRC-MTD-IT",
        failure = Some(
          s"POST of '$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent' returned 400. Response body: ''"
        )
      )
      verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
      verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
    }

    "return a future failed when the invitation creation failed for VAT" in {
      givenNoInvitationsExistForClient(arn, validVrn, "HMRC-MTD-VAT")
      getStatusRelationshipVat(arn.value, validVrn, 404)
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
      failedCreateInvitation(arn)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      an[Exception] shouldBe thrownBy {
        await(result)
      }
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validVrn.value,
        "vrn",
        "Fail",
        "HMRC-MTD-VAT",
        Some(
          s"POST of '$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent' returned 400. Response body: ''"
        )
      )
    }
  }

  "GET /agents/:arn/invitations/:invitationId" when {

    "requesting an ITSA invitation" should {

      val getInvitationItsaApi = controller.getInvitationApi(arn, invitationIdITSA)
      val requestITSA = FakeRequest("GET", s"/agents/${arn.value}/invitations/${invitationIdITSA.value}")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 200 and a json body of a pending invitation" in {

        givenGetITSAInvitationStub(arn, "Pending")
        givenPlatformAnalyticsEventWasSent()

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(pendingItsaInvitation(Service.ItsaMain)).as[JsObject]
        verifyPlatformAnalyticsEventWasSent("get-authorisation-request", Some("HMRC-MTD-IT"))
      }

      "return 200 and a json body of a pending supporting invitation" in {

        givenGetITSASuppInvitationStub(arn, "Pending")
        givenPlatformAnalyticsEventWasSent()

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(pendingItsaInvitation(Service.ItsaSupp)).as[JsObject]
        verifyPlatformAnalyticsEventWasSent("get-authorisation-request", Some("HMRC-MTD-IT-SUPP"))
      }

      "return 200 and a json body of a responded invitation" in {

        givenGetITSAInvitationStub(arn, "Accepted")
        givenPlatformAnalyticsEventWasSent()
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(respondedItsaInvitation(Service.ItsaMain)).as[JsObject]
        verifyPlatformAnalyticsEventWasSent("get-authorisation-request", Some("HMRC-MTD-IT"))
      }

      "return 200 and a json body of a responded supporting invitation" in {

        givenGetITSASuppInvitationStub(arn, "Accepted")
        givenPlatformAnalyticsEventWasSent()
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(respondedItsaInvitation(Service.ItsaSupp)).as[JsObject]
        verifyPlatformAnalyticsEventWasSent("get-authorisation-request", Some("HMRC-MTD-IT-SUPP"))
      }

      "return 403 for Not An Agent" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = getInvitationItsaApi(requestITSA)

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgent
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
        await(result) shouldBe NotAnAgent
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
        await(result) shouldBe AgentNotSubscribed
      }

      "return 403 for No Permission On Agency" in {
        givenGetITSAInvitationStub(arn, "Pending")
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn2.value))
        status(result) shouldBe 403
        await(result) shouldBe NoPermissionOnAgency
      }

      "return 404 for invitation not accessible for this Agent" in {

        givenGetAgentInvitationStubReturns(arn, invitationIdITSA, 403)

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 404
        await(result) shouldBe InvitationNotFound
      }

      "return 404 for Invitation Not Found" in {
        givenInvitationNotFound(arn, invitationIdITSA)
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))
        status(result) shouldBe 404
        await(result) shouldBe InvitationNotFound
      }
    }
    "requesting an VAT invitation" should {
      val getInvitationVatApi = controller.getInvitationApi(arn, invitationIdVAT)
      val requestVAT = FakeRequest("GET", s"/agents/${arn.value}/invitations/${invitationIdVAT.value}")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 200 and a json body of invitation" in {

        givenGetVATInvitationStub(arn, "Pending")
        givenPlatformAnalyticsEventWasSent()
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(pendingVatInvitation).as[JsObject]
        verifyPlatformAnalyticsEventWasSent("get-authorisation-request", Some("HMRC-MTD-VAT"))
      }

      "return 200 and a json body of a responded invitation" in {

        givenGetVATInvitationStub(arn, "Accepted")
        givenPlatformAnalyticsEventWasSent()
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe toJson(respondedVatInvitation).as[JsObject]
        verifyPlatformAnalyticsEventWasSent("get-authorisation-request", Some("HMRC-MTD-VAT"))
      }

      "return 403 for Not An Agent" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = getInvitationVatApi(requestVAT)

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgent
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
        await(result) shouldBe NotAnAgent
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
        await(result) shouldBe AgentNotSubscribed
      }

      "return 403 for No Permission On Agency" in {
        givenGetVATInvitationStub(arn, "Pending")
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn2.value))
        status(result) shouldBe 403
        await(result) shouldBe NoPermissionOnAgency
      }

      "return 404 for Invitation Not Found" in {
        givenInvitationNotFound(arn, invitationIdVAT)
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))
        status(result) shouldBe 404
        await(result) shouldBe InvitationNotFound
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
        givenPlatformAnalyticsEventWasSent()
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))
        status(result) shouldBe 204
        verifyAgentClientInvitationCancelledEvent(arn.value, invitationIdITSA)
        verifyPlatformAnalyticsEventWasSent("cancel-authorisation-request", None)
      }

      "return 403 INVALID_INVITATION_STATUS when the status of the invitation is not Pending" in {
        givenCancelAgentInvitationStubInvalid(arn, invitationIdITSA)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))
        await(result) shouldBe InvalidInvitationStatus
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

        await(result) shouldBe NotAnAgent
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

        await(result) shouldBe AgentNotSubscribed
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisedCancelledViaApi)
      }

      "return 403 NO_PERMISSION_ON_AGENCY when the arn given does not match the logged in user" in {
        givenCancelAgentInvitationStub(arn, invitationIdITSA, 204)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn2.value))
        await(result) shouldBe NoPermissionOnAgency
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisedCancelledViaApi)
      }
    }

    "cancelling a VAT invitation" should {

      val cancelInvitationVatApi = controller.cancelInvitationApi(arn, invitationIdVAT)
      val requestVAT = FakeRequest("DELETE", s"/agents/${arn.value}/invitations/${invitationIdITSA.value}/cancel")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 204 for a successful cancellation" in {
        givenCancelAgentInvitationStub(arn, invitationIdVAT, 204)
        givenPlatformAnalyticsEventWasSent()
        val result = cancelInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))
        status(result) shouldBe 204
        verifyAgentClientInvitationCancelledEvent(arn.value, invitationIdVAT)
        verifyPlatformAnalyticsEventWasSent("cancel-authorisation-request", None)
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
        givenPlatformAnalyticsEventWasSent()
        givenMatchingClientIdAndPostcode(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))
        status(result) shouldBe 204
        verifyPlatformAnalyticsEventWasSent("check-relationship", Some("HMRC-MTD-IT"))
        verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
      }

      "return 204 when the relationship is active for ITSA supporting" in {
        getStatusRelationshipItsa(arn.value, validNino, 200, HMRCMTDITSUPP)
        givenPlatformAnalyticsEventWasSent()
        givenMatchingClientIdAndPostcode(validNino, validPostcode)
        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSASupportingAgentType), arn.value))
        if (itsaSupportingAgentEnabled) {
          status(result) shouldBe 204
          verifyPlatformAnalyticsEventWasSent("check-relationship", Some("HMRC-MTD-IT-SUPP"))
          verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
          verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
        } else {
          status(result) shouldBe 400
          await(result) shouldBe InvalidPayload
          verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
          verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
          verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
        }
      }

      "return 204 when the relationship is active for VAT" in {
        getStatusRelationshipVat(arn.value, validVrn, 200)
        givenPlatformAnalyticsEventWasSent()
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))
        status(result) shouldBe 204
        verifyPlatformAnalyticsEventWasSent("check-relationship", Some("HMRC-MTD-VAT"))
      }

      "return 404 when the relationship is not found for ITSA" in {
        getStatusRelationshipItsa(arn.value, validNino, 404, HMRCMTDIT)
        givenMatchingClientIdAndPostcode(validNino, validPostcode)
        givenPlatformAnalyticsEventWasSent()
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))
        status(result) shouldBe 404
        verifyPlatformAnalyticsEventWasSent("check-relationship", Some("HMRC-MTD-IT"))
        verifyStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDIT)
        verifyNoStatusRelationshipItsaEventWasSent(arn.value, validNino, HMRCMTDITSUPP)
      }

      "return 404 when the relationship is not found for VAT" in {
        getStatusRelationshipVat(arn.value, validVrn, 404)
        givenPlatformAnalyticsEventWasSent()
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))
        status(result) shouldBe 404
        verifyPlatformAnalyticsEventWasSent("check-relationship", Some("HMRC-MTD-VAT"))
      }

      "return 400 SERVICE_NOT_SUPPORTED when the service is not supported" in {
        val jsonBodyInvalidService = Json.parse(
          s"""{"service": ["foo"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe UnsupportedService
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      }

      "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for ITSA" in {
        val jsonBodyInvalidClientId = Json.parse(
          s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "foo", "knownFact": "$validPostcode"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdInvalidFormat
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)

      }

      "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for VAT" in {
        val jsonBodyInvalidClientId = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientType":"business", "clientIdType": "vrn", "clientId": "foo", "knownFact": "$validVatRegDate"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdInvalidFormat
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      }

      "return 400 POSTCODE_FORMAT_INVALID when the postcode has an invalid format" in {
        val jsonBodyInvalidPostcode = Json.parse(
          s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidPostcode), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe PostcodeFormatInvalid
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)

      }

      "return 400 VAT_REG_DATE_FORMAT_INVALID when the VAT registration date has an invalid format" in {
        val jsonBodyInvalidVatRegDate = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientType":"business", "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidVatRegDate), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe VatRegDateFormatInvalid
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)

      }

      "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for ITSA" in {
        val jsonBodyClientIdNotMatchService = Json.parse(
          s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validVrn.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdDoesNotMatchService
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)

      }

      "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for VAT" in {
        val jsonBodyClientIdNotMatchService = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientType":"business", "clientIdType": "vrn", "clientId": "${validNino.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdDoesNotMatchService
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      }

      "return 403 CLIENT_REGISTRATION_NOT_FOUND when the postcode returns nothing" in {
        givenNotEnrolledClientITSA(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe ClientRegistrationNotFound
      }

      "return 403 CLIENT_REGISTRATION_NOT_FOUND when the VAT registration date returns nothing" in {
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 404)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe ClientRegistrationNotFound
      }

      "return 403 POSTCODE_DOES_NOT_MATCH when the postcode and clientId do not match" in {
        givenNonMatchingClientIdAndPostcode(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe PostcodeDoesNotMatch
      }

      "return 403 VAT_REG_DATE_DOES_NOT_MATCH when the VAT registration date and clientId do not match" in {
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 403)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe VatRegDateDoesNotMatch
      }

      "return 403 NOT_AN_AGENT when the logged in user is not have an HMRC-AS-AGENT enrolment" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = checkRelationshipApi(request.withJsonBody(jsonBodyITSA))

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgent
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      }

      "return 403 VAT_CLIENT_INSOLVENT when the when the VAT customer is insolvent" in {
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 403, true)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe VatClientInsolvent
      }

      "return 403 NOT_PERMISSION_ON_AGENCY when the logged in user does not have an HMRC-AS-AGENT enrolment" in {
        createInvitationStub(
          arn,
          validNino.value,
          invitationIdITSA,
          validNino.value,
          "ni",
          "personal",
          "HMRC-MTD-IT",
          "MTDITID",
          validPostcode
        )
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn2.value))

        status(result) shouldBe 403
        await(result) shouldBe NoPermissionOnAgency
        verifyAuditRequestNotSent(AgentAuthorisationEvent.agentAuthorisationCreatedViaApi)
      }
    }

    "GET /agents/:arn/invitations/" when {

      "requesting a sequence of ITSA and VAT invitations" should {

        val getInvitations = controller.getInvitationsApi(arn)
        val request = FakeRequest("GET", s"/agents/${arn.value}/invitations")
          .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

        "return 200 and a json body of a pending invitation filtering out PIR and TERS invitations" in {
          givenInvitationsServiceReturns(arn, Seq(itsa(arn), vat(arn)))
          givenPlatformAnalyticsEventWasSent()
          val result = getInvitations(authorisedAsValidAgent(request, arn.value))

          status(result) shouldBe 200
          Helpers.contentAsJson(result) shouldBe toJson(gettingPendingInvitations(Service.ItsaMain))
          verifyPlatformAnalyticsEventWasSent("get-authorisation-requests", None)
        }

        "return 200 and a json body of a responded invitation IRV and TERS invitations" in {
          givenInvitationsServiceReturns(arn, Seq(irv(arn), ters(arn)))
          givenPlatformAnalyticsEventWasSent()

          intercept[RuntimeException] {
            await(getInvitations(authorisedAsValidAgent(request, arn.value)))
          }.getMessage shouldBe "Unexpected Service has been passed through: PERSONAL-INCOME-RECORD"
          verifyPlatformAnalyticsEventWasSent("get-authorisation-requests", None)
        }

        "return 204 if there are no invitations for the agent" in {
          givenAllInvitationsEmptyStub(arn)
          givenPlatformAnalyticsEventWasSent()
          val result = getInvitations(authorisedAsValidAgent(request, arn.value))

          status(result) shouldBe 204
          verifyPlatformAnalyticsEventWasSent("get-authorisation-requests", None)
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
