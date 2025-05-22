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

import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec

class CancelInvitationControllerISpec extends BaseISpec {

  lazy val controller: CancelInvitationController = app.injector.instanceOf[CancelInvitationController]

  "DELETE /agents/:arn/invitations/:invitationId" when {
    "cancelling an ITSA invitation" should {

      val cancelInvitationItsaApi = controller.cancelInvitation(arn, invitationIdITSA)
      val requestITSA = FakeRequest("DELETE", s"/agent/cancel-invitation/${invitationIdITSA.value}")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 204 for a successful cancellation" in {
        givenCancelAgentInvitationStub(invitationIdITSA, 204)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value)).futureValue
        status(result) shouldBe 204
        result.body shouldBe ""
      }

      "return 403 INVALID_INVITATION_STATUS when ACR responds with this" in {
        givenCancelAgentInvitationStubInvalid(InvalidInvitationStatus, invitationIdITSA)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value)).futureValue
        result shouldBe InvalidInvitationStatus.toResult
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
        val result = cancelInvitationItsaApi(requestITSA).futureValue
        result shouldBe NotAnAgent.toResult
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
        val result = cancelInvitationItsaApi(requestITSA).futureValue

        result shouldBe AgentNotSubscribed.toResult
      }

      "return 403 NO_PERMISSION_ON_AGENCY when the arn given does not match the logged in user" in {
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn2.value)).futureValue
        result shouldBe NoPermissionOnAgency.toResult
      }

      "return 403 NO_PERMISSION_ON_AGENCY when ACR reports arn doesn't match that inside invitation" in {
        givenCancelAgentInvitationStubInvalid(NoPermissionOnAgency, invitationIdITSA)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value)).futureValue
        result shouldBe NoPermissionOnAgency.toResult
      }

      "return 404 INVITATION_NOT_FOUND when the id does not match a stored invitation" in {
        givenCancelAgentInvitationStubInvalid(InvitationNotFound, invitationIdITSA)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value)).futureValue
        result shouldBe InvitationNotFound.toResult
      }

    }
  }
}
