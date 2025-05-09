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

import play.api.test.Helpers._
import uk.gov.hmrc.agentauthorisation.models.Service.{ItsaMain, ItsaSupp, Vat}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

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
        validPostcode
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
        validPostcode
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
        validVatRegDate
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
        validPostcode
      )
      val result = await(connector.createInvitation(arn, testItsaInvite))
      result shouldBe Left(ClientRegistrationNotFound)
    }
  }

}
