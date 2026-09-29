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

import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentauthorisation.models.NoPermissionOnAgency
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, Resource}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import scala.concurrent.ExecutionContext.Implicits.global

class LegacyCancelActionRoutingISpec extends BaseISpec:

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> true)

  private given WSClient = app.injector.instanceOf[WSClient]

  private def cancelInvitation(authenticatedArn: String) =
    given HeaderCarrier = HeaderCarrier(
      authorization = Some(Authorization("Bearer XYZ")),
      otherHeaders = Seq("Accept" -> "application/vnd.hmrc.2.0+json")
    )
    givenAuthorisedAsValidAgent(authenticatedArn)

    new Resource(s"/agents/${arn.value}/invitations/${invitationIdITSA.value}", port).delete()

  "the routed legacy DELETE invitation endpoint with V3 enabled" should:
    "still cancel an invitation for the authorised agent" in:
      givenCancelAgentInvitationStub(invitationIdITSA, 204)

      val response = cancelInvitation(arn.value)

      response.status shouldBe 204
      response.body shouldBe empty

    "still reject a route ARN that differs from the authenticated ARN" in:
      val response = cancelInvitation(arn2.value)

      response.status shouldBe 403
      response.json shouldBe NoPermissionOnAgency.toJson
