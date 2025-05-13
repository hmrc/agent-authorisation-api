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
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.http.SessionKeys

class GetInvitationsControllerISpec extends BaseISpec {
  lazy val controller: GetInvitationsController = app.injector.instanceOf[GetInvitationsController]

  lazy val configuration: Configuration = app.injector.instanceOf[Configuration]

  val itsaSupportingAgentEnabled = configuration.get[Boolean]("itsa-supporting-agent.enabled")

  implicit val writerRespondedInvitation =
    RespondedInvitation.writesExternalWithAgentType

  implicit val writerPendingInvitation =
    PendingInvitation.writesExternalWithAgentType

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

      "return 403 when auth arn does not match agent arn" in {
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn2.value))

        status(result) shouldBe 403
        await(result) shouldBe NoPermissionOnAgencyResult
      }

      "return 403 for invitation belonging to another Agent" in {
        givenGetAgentInvitationStubReturns(arn, invitationIdITSA, 403, "NO_PERMISSION_ON_AGENCY")

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 403
        await(result) shouldBe NoPermissionOnAgencyResult
      }

      "return 404 for Invitation Not Found" in {
        givenGetAgentInvitationStubReturns(arn, invitationIdITSA, 404, "INVITATION_NOT_FOUND")

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

      "return 403 for invitation belonging to another Agent" in {
        givenGetAgentInvitationStubReturns(arn, invitationIdVAT, 403, "NO_PERMISSION_ON_AGENCY")

        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 403
        await(result) shouldBe NoPermissionOnAgencyResult
      }

      "return 404 for Invitation Not Found" in {
        givenGetAgentInvitationStubReturns(arn, invitationIdVAT, 403, "INVITATION_NOT_FOUND")

        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 404
        await(result) shouldBe InvitationNotFoundResult
      }
    }
  }
}
