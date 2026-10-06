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

package uk.gov.hmrc.agentauthorisation.models.v3

import play.api.libs.json.{Json, OWrites, Reads}
import uk.gov.hmrc.agentauthorisation.models.{AgentType, ApiService}

import java.time.{Instant, LocalDate}

final case class AcrInvitationInfoV3(
  agentLink: AcrAgentLinkV3,
  authorisationRequest: AcrInvitationV3
)

object AcrInvitationInfoV3:
  given Reads[AcrInvitationInfoV3] = Json.reads[AcrInvitationInfoV3]

final case class GetInvitationByIdV3Response(
  invitationId: String,
  service: ApiService,
  status: ApiInvitationStatus,
  created: Instant,
  updated: Option[Instant],
  expiresOn: Option[LocalDate],
  clientActionUrl: Option[String],
  agentType: Option[AgentType]
)

object GetInvitationByIdV3Response:
  given OWrites[GetInvitationByIdV3Response] = OWrites: invitation =>
    Json.obj(
      "invitationId" -> invitation.invitationId,
      "service"      -> invitation.service.value,
      "status"       -> invitation.status,
      "created"      -> invitation.created
    ) ++ invitation.updated.fold(Json.obj())(value => Json.obj("updated" -> value)) ++
      invitation.expiresOn.fold(Json.obj())(value => Json.obj("expiresOn" -> value)) ++
      invitation.clientActionUrl.fold(Json.obj())(value => Json.obj("clientActionUrl" -> value)) ++
      invitation.agentType.fold(Json.obj())(value => Json.obj("agentType" -> value))
