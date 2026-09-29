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

import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentauthorisation.models.{ApiVersion, Service}
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, Resource, TestInvitation}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import scala.concurrent.ExecutionContext.Implicits.global

abstract class LegacyAcceptRoutingISpec extends BaseISpec:

  protected def v3Enabled: Boolean

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> v3Enabled)

  protected given WSClient = app.injector.instanceOf[WSClient]

  protected def getInvitation(accept: Option[String]) =
    given HeaderCarrier = HeaderCarrier(
      authorization = Some(Authorization("Bearer XYZ")),
      otherHeaders = accept.toSeq.map("Accept" -> _)
    )
    givenGetAgentInvitationStub(arn, TestInvitation(invitationIdITSA, serviceITSA, "Pending"))
    givenAuthorisedAsValidAgent(arn.value)

    new Resource(s"/agents/${arn.value}/invitations/${invitationIdITSA.value}", port).get()

  protected def getInvitations(accept: Option[String]) =
    given HeaderCarrier = HeaderCarrier(
      authorization = Some(Authorization("Bearer XYZ")),
      otherHeaders = accept.toSeq.map("Accept" -> _)
    )
    givenGetAllAgentInvitationsStub(arn, Seq(TestInvitation(invitationIdITSA, serviceITSA, "Pending")))
    givenAuthorisedAsValidAgent(arn.value)

    new Resource(s"/agents/${arn.value}/invitations", port).get()

  "the routed legacy invitation endpoint" should:
    "continue without an Accept header" in:
      getInvitation(None).status shouldBe 200

    "continue with a non-HMRC Accept header" in:
      getInvitation(Some("text/plain")).status shouldBe 200

    "continue with an unsupported HMRC version" in:
      getInvitation(Some("application/vnd.hmrc.9.0+json")).status shouldBe 200

class LegacyAcceptRoutingV3OffISpec extends LegacyAcceptRoutingISpec:
  override protected def v3Enabled: Boolean = false

  "the disabled V3 switch" should:
    "leave the exact V3 Accept header on the legacy handler" in:
      getInvitation(Some(ApiVersion.V3AcceptHeader)).status shouldBe 200

class LegacyAcceptRoutingV3OnISpec extends LegacyAcceptRoutingISpec:
  override protected def v3Enabled: Boolean = true

  "the routed legacy invitation create with V3 enabled" should:
    "return the existing response for a V2 Accept header" in:
      given HeaderCarrier = HeaderCarrier(
        authorization = Some(Authorization("Bearer XYZ")),
        otherHeaders = Seq("Accept" -> "application/vnd.hmrc.2.0+json")
      )
      givenAuthorisedAsValidAgent(arn.value)
      createInvitationStub(arn, invitationIdITSA, Service.ItsaMain, validNino.value, validPostcode, "personal")

      val payload = Json.obj(
        "service" -> Json.arr("MTD-IT"),
        "clientType" -> "personal",
        "clientIdType" -> "ni",
        "clientId" -> validNino.value,
        "knownFact" -> validPostcode
      )
      val response = new Resource(s"/agents/${arn.value}/invitations", port).postAsJson(payload.toString())

      response.status shouldBe 204
      response.header("Location") shouldBe Some(s"/agents/${arn.value}/invitations/${invitationIdITSA.value}")

  "the routed legacy invitation list with V3 enabled" should:
    "return the existing response for a V2 Accept header" in:
      val response = getInvitations(Some("application/vnd.hmrc.2.0+json"))

      response.status shouldBe 200
      response.json.as[JsArray].value should have size 1
