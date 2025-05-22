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
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, TestInvitation}
import uk.gov.hmrc.http.SessionKeys

class GetInvitationsControllerISpec extends BaseISpec {
  lazy val controller: GetInvitationsController = app.injector.instanceOf[GetInvitationsController]

  def pendingItsaInvitation(service: Service): JsObject = Json.obj(
    "_links"    -> Json.obj("self" -> Json.obj("href" -> "/agents/TARN0000001/invitations/ABERULMHCKKW3")),
    "created"   -> "2017-10-31T23:22:50.971Z",
    "expiresOn" -> "2017-12-18T00:00:00.000",
    "arn"       -> "TARN0000001",
    "service"   -> Json.arr("MTD-IT"),
    "status"    -> "Pending",
    "clientActionUrl" -> "http://localhost:9435/agent-client-relationships/appoint-someone-to-deal-with-HMRC-for-you/12345678/agent-1/income-tax",
    "agentType" -> service.agentType
  )

  def respondedItsaInvitation(service: Service): JsObject = Json.obj(
    "_links"    -> Json.obj("self" -> Json.obj("href" -> "/agents/TARN0000001/invitations/ABERULMHCKKW3")),
    "created"   -> "2017-10-31T23:22:50.971Z",
    "updated"   -> "2018-09-11T21:02:50.123Z",
    "arn"       -> "TARN0000001",
    "service"   -> Json.arr("MTD-IT"),
    "status"    -> "Accepted",
    "agentType" -> service.agentType
  )

  val pendingVatInvitation: JsObject = Json.obj(
    "_links"    -> Json.obj("self" -> Json.obj("href" -> "/agents/TARN0000001/invitations/CZTW1KY6RTAAT")),
    "created"   -> "2017-10-31T23:22:50.971Z",
    "expiresOn" -> "2017-12-18T00:00:00.000",
    "arn"       -> "TARN0000001",
    "service"   -> Json.arr("MTD-VAT"),
    "status"    -> "Pending",
    "clientActionUrl" -> "http://localhost:9435/agent-client-relationships/appoint-someone-to-deal-with-HMRC-for-you/12345678/agent-1/vat"
  )

  val respondedVatInvitation: JsObject = Json.obj(
    "_links"  -> Json.obj("self" -> Json.obj("href" -> "/agents/TARN0000001/invitations/CZTW1KY6RTAAT")),
    "created" -> "2017-10-31T23:22:50.971Z",
    "updated" -> "2018-09-11T21:02:50.123Z",
    "arn"     -> "TARN0000001",
    "service" -> Json.arr("MTD-VAT"),
    "status"  -> "Accepted"
  )

  val multipleInvitationsJson: JsArray = Json
    .arr(
      Json.obj(
        "_links"    -> Json.obj("self" -> Json.obj("href" -> "/agents/TARN0000001/invitations/ABERULMHCKKW3")),
        "created"   -> "2017-10-31T23:22:50.971Z",
        "expiresOn" -> "2017-12-18T00:00:00.000",
        "arn"       -> "TARN0000001",
        "service"   -> Json.arr("MTD-IT"),
        "status"    -> "Pending",
        "clientActionUrl" -> "http://localhost:9435/agent-client-relationships/appoint-someone-to-deal-with-HMRC-for-you/12345678/agent-1/income-tax",
        "agentType" -> "main"
      ),
      Json.obj(
        "_links"  -> Json.obj("self" -> Json.obj("href" -> "/agents/TARN0000001/invitations/CZTW1KY6RTAAT")),
        "created" -> "2017-10-31T23:22:50.971Z",
        "updated" -> "2018-09-11T21:02:50.123Z",
        "arn"     -> "TARN0000001",
        "service" -> Json.arr("MTD-VAT"),
        "status"  -> "Accepted"
      )
    )

  "GET /agents/:arn/invitations/:invitationId" when {
    "requesting an ITSA invitation" should {
      val getInvitationItsaApi = controller.getInvitationApi(arn, invitationIdITSA)
      val requestITSA = FakeRequest("GET", s"/agents/${arn.value}/invitations/${invitationIdITSA.value}")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 200 and a json body of a pending invitation" in {
        givenGetAgentInvitationStub(arn, TestInvitation(invitationIdITSA, serviceITSA, "Pending"))

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe pendingItsaInvitation(Service.ItsaMain)
      }

      "return 200 and a json body of a pending supporting invitation" in {
        givenGetAgentInvitationStub(arn, TestInvitation(invitationIdITSA, serviceITSASupp, "Pending"))

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe pendingItsaInvitation(Service.ItsaSupp)
      }

      "return 200 and a json body of a responded invitation" in {
        givenGetAgentInvitationStub(arn, TestInvitation(invitationIdITSA, serviceITSA, "Accepted"))

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe respondedItsaInvitation(Service.ItsaMain)
      }

      "return 200 and a json body of a responded supporting invitation" in {
        givenGetAgentInvitationStub(arn, TestInvitation(invitationIdITSA, serviceITSASupp, "Accepted"))

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe respondedItsaInvitation(Service.ItsaSupp)
      }

      "return 403 for Not An Agent" in {
        givenUnauthorisedForInsufficientEnrolments()

        val result = getInvitationItsaApi(requestITSA)

        status(result) shouldBe 403
        Helpers.contentAsJson(result) shouldBe NotAnAgent.toJson
      }

      "return 403 for Not an Agent" in {
        givenGetAgentInvitationStub(arn, TestInvitation(invitationIdITSA, serviceITSA, "Pending"))
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
        Helpers.contentAsJson(result) shouldBe NotAnAgent.toJson
      }

      "return 403 for Agent Not Subscribed" in {
        givenGetAgentInvitationStub(arn, TestInvitation(invitationIdITSA, serviceITSA, "Pending"))
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
        Helpers.contentAsJson(result) shouldBe AgentNotSubscribed.toJson
      }

      "return 403 when auth arn does not match agent arn" in {
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn2.value))

        status(result) shouldBe 403
        Helpers.contentAsJson(result) shouldBe NoPermissionOnAgency.toJson
      }

      "return 403 for invitation belonging to another Agent" in {
        givenGetAgentInvitationStubError(arn, invitationIdITSA, 403, Some("NO_PERMISSION_ON_AGENCY"))

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 403
        Helpers.contentAsJson(result) shouldBe NoPermissionOnAgency.toJson
      }

      "return 404 for Invitation Not Found" in {
        givenGetAgentInvitationStubError(arn, invitationIdITSA, 404, Some("INVITATION_NOT_FOUND"))

        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 404
        Helpers.contentAsJson(result) shouldBe InvitationNotFound.toJson
      }
    }

    "requesting an VAT invitation" should {
      val getInvitationVatApi = controller.getInvitationApi(arn, invitationIdVAT)
      val requestVAT = FakeRequest("GET", s"/agents/${arn.value}/invitations/${invitationIdVAT.value}")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 200 and a json body of invitation" in {
        givenGetAgentInvitationStub(arn, TestInvitation(invitationIdVAT, serviceVAT, "Pending"))

        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe pendingVatInvitation
      }

      "return 200 and a json body of a responded invitation" in {
        givenGetAgentInvitationStub(arn, TestInvitation(invitationIdVAT, serviceVAT, "Accepted"))

        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe respondedVatInvitation
      }

      "return 403 for Not An Agent" in {
        givenUnauthorisedForInsufficientEnrolments()

        val result = getInvitationVatApi(requestVAT)

        status(result) shouldBe 403
        Helpers.contentAsJson(result) shouldBe NotAnAgent.toJson
      }

      "return 403 for Not an Agent" in {
        givenGetAgentInvitationStub(arn, TestInvitation(invitationIdVAT, serviceVAT, "Pending"))
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
        Helpers.contentAsJson(result) shouldBe NotAnAgent.toJson
      }

      "return 403 for Agent Not Subscribed" in {
        givenGetAgentInvitationStub(arn, TestInvitation(invitationIdVAT, serviceVAT, "Pending"))
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
        Helpers.contentAsJson(result) shouldBe AgentNotSubscribed.toJson
      }

      "return 403 for No Permission On Agency" in {
        givenGetAgentInvitationStub(arn, TestInvitation(invitationIdVAT, serviceVAT, "Pending"))

        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn2.value))

        status(result) shouldBe 403
        Helpers.contentAsJson(result) shouldBe NoPermissionOnAgency.toJson
      }

      "return 403 for invitation belonging to another Agent" in {
        givenGetAgentInvitationStubError(arn, invitationIdVAT, 403, Some("NO_PERMISSION_ON_AGENCY"))

        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 403
        Helpers.contentAsJson(result) shouldBe NoPermissionOnAgency.toJson
      }

      "return 404 for Invitation Not Found" in {
        givenGetAgentInvitationStubError(arn, invitationIdVAT, 403, Some("INVITATION_NOT_FOUND"))

        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 404
        Helpers.contentAsJson(result) shouldBe InvitationNotFound.toJson
      }
    }
  }

  "GET /agents/:arn/invitations/" when {
    "requesting all API supported invitations" should {
      val getInvitations = controller.getInvitationsApi(arn)
      val request = FakeRequest("GET", s"/agents/${arn.value}/invitations")
        .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")

      "return 200 and a json body of a pending invitations" in {
        givenGetAllAgentInvitationsStub(
          arn,
          Seq(
            TestInvitation(invitationIdITSA, serviceITSA, "Pending"),
            TestInvitation(invitationIdVAT, serviceVAT, "Accepted")
          )
        )

        val result = getInvitations(authorisedAsValidAgent(request, arn.value))

        status(result) shouldBe 200
        Helpers.contentAsJson(result) shouldBe multipleInvitationsJson
      }

      "return 204 if there are no invitations for the agent" in {
        givenGetAllAgentInvitationsStub(arn, Nil)

        val result = getInvitations(authorisedAsValidAgent(request, arn.value))

        status(result) shouldBe 204
        Helpers.contentAsString(result) shouldBe ""
      }
    }
  }

}
