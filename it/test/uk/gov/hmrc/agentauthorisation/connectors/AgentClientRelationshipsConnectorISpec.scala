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

package uk.gov.hmrc.agentauthorisation.connectors

import uk.gov.hmrc.agentauthorisation.models.Service.{ItsaMain, ItsaSupp, Vat}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDate}

class AgentClientRelationshipsConnectorISpec extends BaseISpec {

  val connector: AgentClientRelationshipsConnector = app.injector.instanceOf[AgentClientRelationshipsConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testItsaInvite = CreateInvitationRequestToAcr(ItsaMain, "AB123456A", "DH14EJ", "personal")

  "createInvitation" should {
    "return a Invitation Id upon success for ITSA" in {
      createInvitationStub(
        arn,
        invitationIdITSA,
        ItsaMain,
        validNino.value,
        validPostcode,
        "personal"
      )
      val result = connector.createInvitation(arn, testItsaInvite).futureValue
      result shouldBe Right(invitationIdITSA)
    }

    "return a Invitation Id upon success for ITSA supporting" in {
      createInvitationStub(
        arn,
        invitationIdITSA,
        ItsaSupp,
        validNino.value,
        validPostcode,
        "personal"
      )
      val result = connector.createInvitation(arn, testItsaInvite.copy(service = ItsaSupp)).futureValue
      result shouldBe Right(invitationIdITSA)
    }

    "return a Invitation Id upon success for VAT" in {
      createInvitationStub(
        arn,
        invitationIdVAT,
        Service.Vat,
        validVrn.value,
        validVatRegDate,
        "business"
      )
      val agentInvitation = CreateInvitationRequestToAcr(Vat, validVrn.value, validVatRegDate, "business")
      val result = connector.createInvitation(arn, agentInvitation).futureValue
      result shouldBe Right(invitationIdVAT)
    }

    "return an error as found in ACR" in {
      createInvitationErrorStub(
        error = ClientRegistrationNotFound,
        arn,
        invitationIdITSA,
        ItsaMain,
        validNino.value,
        validPostcode,
        "personal"
      )
      val result = connector.createInvitation(arn, testItsaInvite).futureValue
      result shouldBe Left(ClientRegistrationNotFound)
    }
  }

  "getInvitation" should {
    "return invitation details received from ACR" in {
      givenGetITSAInvitationStub(arn, "Pending")

      val result = connector.getInvitation(arn, invitationIdITSA).futureValue

      result shouldBe Right(
        InvitationDetails(
          "12345678",
          "agent-1",
          Instant.parse("2017-10-31T23:22:50.971Z"),
          ItsaMain,
          "Pending",
          LocalDate.parse("2017-12-18"),
          "ABERULMHCKKW3",
          Instant.parse("2018-09-11T21:02:50.123Z")
        )
      )
    }
    "return an error as found in ACR" in {
      givenGetAgentInvitationStubReturns(arn, invitationIdITSA, 404, Some("INVITATION_NOT_FOUND"))

      val result = connector.getInvitation(arn, invitationIdITSA).futureValue

      result shouldBe Left(InvitationNotFound)
    }
  }
  "cancelInvitation" should {
    "return 204 when cancellation is successful" in {
      givenCancelAgentInvitationStub(invitationIdITSA, 204)
      val result = connector.cancelInvitation(invitationIdITSA).futureValue

      result shouldBe Right(204)
    }

    "return 404 when invitation is not found" in {
      givenCancelAgentInvitationStubInvalid(InvitationNotFound, invitationIdITSA)
      val result = connector.cancelInvitation(invitationIdITSA).futureValue

      result shouldBe Left(InvitationNotFound)
    }

    "return 500 when an invitation cannot be cancelled" in {
      givenCancelAgentInvitationStubInvalid(StandardInternalServerError, invitationIdITSA)
      val result = connector.cancelInvitation(invitationIdITSA).futureValue

      result shouldBe Left(StandardInternalServerError)
    }

    "return 403 when ACR tells us ARN does not match" in {
      givenCancelAgentInvitationStubInvalid(NoPermissionOnAgency, invitationIdITSA)
      val result = connector.cancelInvitation(invitationIdITSA).futureValue

      result shouldBe Left(NoPermissionOnAgency)
    }
  }

}
