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

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, Resource, TestInvitation}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global

abstract class LegacyRoutesISpec extends BaseISpec {

  protected def v3Enabled: Boolean

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> v3Enabled)

  private given WSClient = app.injector.instanceOf[WSClient]

  private val createPayload = Json.obj(
    "service"      -> Json.arr("MTD-IT"),
    "clientType"   -> "personal",
    "clientIdType" -> "ni",
    "clientId"     -> validNino.value,
    "knownFact"    -> validPostcode
  )

  private val relationshipPayload = Json.obj(
    "service"      -> Json.arr("MTD-IT"),
    "clientIdType" -> "ni",
    "clientId"     -> validNino.value,
    "knownFact"    -> validPostcode
  )

  private val deauthorisePayload = Json.obj(
    "service"      -> Json.arr("MTD-IT"),
    "clientType"   -> "personal",
    "clientIdType" -> "ni",
    "clientId"     -> validNino.value
  )

  private val itsaClientAccessData = ClientAccessData(
    service = Service.ItsaMain,
    suppliedClientId = validNino.value,
    knownFact = validPostcode,
    clientType = None
  )

  private val pendingItsaInvitation = Json.obj(
    "_links" -> Json.obj(
      "self" -> Json.obj("href" -> s"/agents/${arn.value}/invitations/${invitationIdITSA.value}")
    ),
    "created"   -> "2017-10-31T23:22:50.971Z",
    "expiresOn" -> "2017-12-18T00:00:00.000",
    "arn"       -> arn.value,
    "service"   -> Json.arr("MTD-IT"),
    "status"    -> "Pending",
    "clientActionUrl" ->
      "http://localhost:9435/agent-client-relationships/appoint-someone-to-deal-with-HMRC-for-you/12345678/agent-1/income-tax",
    "agentType" -> "main"
  )

  private def requestHeaders(version: String = "2.0"): HeaderCarrier =
    HeaderCarrier(
      authorization = Some(Authorization("Bearer XYZ")),
      otherHeaders = Seq("Accept" -> s"application/vnd.hmrc.$version+json")
    )

  private def authorisedRequest(version: String = "2.0", authenticatedArn: String = arn.value): HeaderCarrier = {
    givenAuthorisedAsValidAgent(authenticatedArn)
    requestHeaders(version)
  }

  private def currentRoutes(using HeaderCarrier): Seq[(String, () => HttpResponse)] = Seq(
    "list invitations" -> (() => new Resource(s"/agents/${arn.value}/invitations", port).get()),
    "create invitation" -> (() => new Resource(s"/agents/${arn.value}/invitations", port).postAsJson("{}")),
    "get invitation" -> (() => new Resource(s"/agents/${arn.value}/invitations/${invitationIdITSA.value}", port).get()),
    "cancel invitation" -> (() => new Resource(s"/agents/${arn.value}/invitations/${invitationIdITSA.value}", port).delete()),
    "check relationship" -> (() => new Resource(s"/agents/${arn.value}/relationships", port).postAsJson("{}")),
    "deauthorise client" -> (() => new Resource(s"/agents/${arn.value}/deauthorise-client", port).putAsJson("{}"))
  )

  "the routed V1/V2 API" should {

    "preserve create-invitation behaviour" in {
      createInvitationStub(
        arn,
        invitationIdITSA,
        Service.ItsaMain,
        validNino.value,
        validPostcode,
        "personal"
      )

      given HeaderCarrier = authorisedRequest()
      val response = new Resource(s"/agents/${arn.value}/invitations", port).postAsJson(createPayload.toString())

      response.status shouldBe 204
      response.body shouldBe empty
      response.header("Location") shouldBe Some(s"/agents/${arn.value}/invitations/${invitationIdITSA.value}")
    }

    "preserve list-invitations behaviour and restore the API Platform context path" in {
      givenGetAllAgentInvitationsStub(
        arn,
        Seq(TestInvitation(invitationIdITSA, serviceITSA, "Pending"))
      )

      given HeaderCarrier = authorisedRequest(version = "1.0")
      val response = new Resource(s"/${arn.value}/invitations", port).get()

      response.status shouldBe 200
      response.json shouldBe Json.arr(pendingItsaInvitation)
    }

    "preserve get-invitation behaviour" in {
      givenGetAgentInvitationStub(arn, TestInvitation(invitationIdITSA, serviceITSA, "Pending"))

      given HeaderCarrier = authorisedRequest()
      val response = new Resource(
        s"/agents/${arn.value}/invitations/${invitationIdITSA.value}",
        port
      ).get()

      response.status shouldBe 200
      response.json shouldBe pendingItsaInvitation
    }

    "preserve cancel-invitation behaviour" in {
      givenCancelAgentInvitationStub(invitationIdITSA, 204)

      given HeaderCarrier = authorisedRequest()
      val response = new Resource(
        s"/agents/${arn.value}/invitations/${invitationIdITSA.value}",
        port
      ).delete()

      response.status shouldBe 204
      response.body shouldBe empty
    }

    "preserve check-relationship behaviour" in {
      givenCheckRelationshipStub(
        arn = arn.value,
        status = 204,
        clientAccessData = itsaClientAccessData
      )

      given HeaderCarrier = authorisedRequest()
      val response = new Resource(s"/agents/${arn.value}/relationships", port)
        .postAsJson(relationshipPayload.toString())

      response.status shouldBe 204
      response.body shouldBe empty
    }

    "preserve deauthorise-client behaviour" in {
      givenRemoveAuthorisationStub(
        arn = arn,
        clientId = validNino.value,
        service = Service.ItsaMain.internalServiceName,
        status = 204
      )

      given HeaderCarrier = authorisedRequest()
      val response = new Resource(s"/agents/${arn.value}/deauthorise-client", port)
        .putAsJson(deauthorisePayload.toString())

      response.status shouldBe 204
      response.body shouldBe empty
    }

    "preserve NOT_AN_AGENT" in {
      givenUnauthorisedForInsufficientEnrolments()

      Seq("2.0", "3.0").foreach { version =>
        given HeaderCarrier = requestHeaders(version)
        currentRoutes.foreach { case (description, sendRequest) =>
          withClue(s"$version $description") {
            val response = sendRequest()
            response.status shouldBe 403
            response.json shouldBe NotAnAgent.toJson
          }
        }
      }
    }

    "preserve AGENT_NOT_SUBSCRIBED" in {
      givenAuthenticatedAgent(Enrolment("IR-SA-AGENT", "IRAgentReference", "someIRAR"))

      Seq("2.0", "3.0").foreach { version =>
        given HeaderCarrier = requestHeaders(version)
        currentRoutes.foreach { case (description, sendRequest) =>
          withClue(s"$version $description") {
            val response = sendRequest()
            response.status shouldBe 403
            response.json shouldBe AgentNotSubscribed.toJson
          }
        }
      }
    }

    "preserve NO_PERMISSION_ON_AGENCY" in {
      givenAuthorisedAsValidAgent(arn2.value)

      Seq("2.0", "3.0").foreach { version =>
        given HeaderCarrier = requestHeaders(version)
        currentRoutes.foreach { case (description, sendRequest) =>
          withClue(s"$version $description") {
            val response = sendRequest()
            response.status shouldBe 403
            response.json shouldBe NoPermissionOnAgency.toJson
          }
        }
      }
    }
  }
}

class LegacyRoutesV3OffISpec extends LegacyRoutesISpec {
  override protected def v3Enabled: Boolean = false
}

class LegacyRoutesV3OnISpec extends LegacyRoutesISpec {
  override protected def v3Enabled: Boolean = true
}
