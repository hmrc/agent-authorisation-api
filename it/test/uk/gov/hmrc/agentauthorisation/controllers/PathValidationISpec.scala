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
import uk.gov.hmrc.agentauthorisation.models.{ApiVersion, ArnInvalidFormat, InvitationIdInvalidFormat}
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, Resource}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

abstract class PathValidationISpec extends BaseISpec:

  protected def v3Enabled: Boolean
  protected def acceptHeader: String

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> v3Enabled)

  private given WSClient = app.injector.instanceOf[WSClient]
  private given HeaderCarrier = HeaderCarrier(otherHeaders = Seq("Accept" -> acceptHeader))

  "the routed API" should:
    "return ARN_FORMAT_INVALID before invoking each current controller" in:
      val invalidArn = "not-an-arn"
      val requests = Seq(
        "list invitations"  -> (() => new Resource(s"/agents/$invalidArn/invitations", port).get()),
        "create invitation" -> (() => new Resource(s"/agents/$invalidArn/invitations", port).postAsJson("{}")),
        "get invitation" -> (() =>
          new Resource(s"/agents/$invalidArn/invitations/ABERULMHCKKW3", port).get()
        ),
        "cancel invitation" -> (() =>
          new Resource(s"/agents/$invalidArn/invitations/ABERULMHCKKW3", port).delete()
        ),
        "check relationship" -> (() => new Resource(s"/agents/$invalidArn/relationships", port).postAsJson("{}")),
        "deauthorise client" -> (() => new Resource(s"/agents/$invalidArn/deauthorise-client", port).putAsJson("{}"))
      )

      requests.foreach: (description, sendRequest) =>
        withClue(description):
          val response = sendRequest()
          response.status shouldBe 400
          response.json shouldBe ArnInvalidFormat.toJson

    "return INVITATION_ID_FORMAT_INVALID for current invitation routes" in:
      val invalidInvitationId = "not-an-invitation-id"
      val requests = Seq(
        "get invitation" -> (() =>
          new Resource(s"/agents/${arn.value}/invitations/$invalidInvitationId", port).get()
        ),
        "cancel invitation" -> (() =>
          new Resource(s"/agents/${arn.value}/invitations/$invalidInvitationId", port).delete()
        )
      )

      requests.foreach: (description, sendRequest) =>
        withClue(description):
          val response = sendRequest()
          response.status shouldBe 400
          response.json shouldBe InvitationIdInvalidFormat.toJson

class PathValidationV3OffV2ISpec extends PathValidationISpec:
  override protected def v3Enabled: Boolean = false
  override protected def acceptHeader: String = "application/vnd.hmrc.2.0+json"

class PathValidationV3OffV3ISpec extends PathValidationISpec:
  override protected def v3Enabled: Boolean = false
  override protected def acceptHeader: String = ApiVersion.V3AcceptHeader

class PathValidationV3OnV3ISpec extends PathValidationISpec:
  override protected def v3Enabled: Boolean = true
  override protected def acceptHeader: String = ApiVersion.V3AcceptHeader
