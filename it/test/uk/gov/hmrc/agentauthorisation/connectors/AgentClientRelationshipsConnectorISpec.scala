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

import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.models.Service.{ItsaMain, ItsaSupp, Vat}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, TestInvitation}

import java.time.{Instant, LocalDate}

class AgentClientRelationshipsConnectorISpec extends BaseISpec {

  val connector: AgentClientRelationshipsConnector = app.injector.instanceOf[AgentClientRelationshipsConnector]

  private implicit val request: RequestHeader = FakeRequest()

  val testClientAccessData = ClientAccessData(ItsaMain, "AB123456A", "DH14EJ", "personal")

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
      val result = connector.createInvitation(arn, testClientAccessData).futureValue
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
      val result = connector.createInvitation(arn, testClientAccessData.copy(service = ItsaSupp)).futureValue
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
      val agentInvitation = ClientAccessData(Vat, validVrn.value, validVatRegDate, "business")
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
      val result = connector.createInvitation(arn, testClientAccessData).futureValue
      result shouldBe Left(ClientRegistrationNotFound)
    }
  }

  "getInvitation" should {
    "return invitation details received from ACR" in {
      givenGetAgentInvitationStub(arn, TestInvitation(invitationIdITSA, serviceITSA, "Pending"))

      val result = connector.getInvitation(arn, invitationIdITSA).futureValue

      result shouldBe Right(
        SingleInvitationDetails(
          AgentDetails("12345678", "agent-1"),
          InvitationDetails(
            Instant.parse("2017-10-31T23:22:50.971Z"),
            ItsaMain,
            "Pending",
            LocalDate.parse("2017-12-18"),
            "ABERULMHCKKW3",
            Instant.parse("2018-09-11T21:02:50.123Z")
          )
        )
      )
    }
    "return an error as found in ACR" in {
      givenGetAgentInvitationStubError(arn, invitationIdITSA, 404, Some("INVITATION_NOT_FOUND"))

      val result = connector.getInvitation(arn, invitationIdITSA).futureValue

      result shouldBe Left(InvitationNotFound)
    }
  }

  "getAllInvitations" should {
    "return a sequence of stored invitations" in {
      givenGetAllAgentInvitationsStub(
        arn,
        Seq(
          TestInvitation(invitationIdITSA, serviceITSA, "Pending"),
          TestInvitation(invitationIdVAT, serviceVAT, "Accepted")
        )
      )

      val result = connector.getAllInvitations(arn).futureValue

      result shouldBe Right(
        AllInvitationDetails(
          AgentDetails("12345678", "agent-1"),
          Seq(
            InvitationDetails(
              Instant.parse("2017-10-31T23:22:50.971Z"),
              ItsaMain,
              "Pending",
              LocalDate.parse("2017-12-18"),
              "ABERULMHCKKW3",
              Instant.parse("2018-09-11T21:02:50.123Z")
            ),
            InvitationDetails(
              Instant.parse("2017-10-31T23:22:50.971Z"),
              Vat,
              "Accepted",
              LocalDate.parse("2017-12-18"),
              "CZTW1KY6RTAAT",
              Instant.parse("2018-09-11T21:02:50.123Z")
            )
          )
        )
      )
    }

    "return a empty sequence of stored invitations" in {
      givenGetAllAgentInvitationsStub(arn, Nil)

      val result = connector.getAllInvitations(arn).futureValue

      result shouldBe Right(AllInvitationDetails(AgentDetails("12345678", "agent-1"), Nil))
    }

    "return an error as found in ACR" in {
      givenGetAgentAllInvitationsStubError(arn, 403, Some("AGENT_SUSPENDED"))

      val result = connector.getAllInvitations(arn).futureValue

      result shouldBe Left(NoPermissionOnAgency)
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

  "checkRelationship" should {
    val itsaClientAccessData = ClientAccessData(
      service = ItsaMain,
      suppliedClientId = validNino.value,
      knownFact = validPostcode,
      clientType = "personal"
    )
    "return true when a relationship is found" in {
      givenCheckRelationshipStub(
        arn = arn.value,
        status = 204,
        optCode = None,
        clientAccessData = itsaClientAccessData
      )
      val result = connector.checkRelationship(arn, testClientAccessData).futureValue

      result shouldBe Right(true)
    }
  }
}
