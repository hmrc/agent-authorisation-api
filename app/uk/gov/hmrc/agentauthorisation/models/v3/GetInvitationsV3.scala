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

import play.api.libs.json.*
import uk.gov.hmrc.agentauthorisation.models.{AgentType, ApiService}

import java.time.{Instant, LocalDate}

enum ApiInvitationStatus(val value: String):
  case Pending extends ApiInvitationStatus("Pending")
  case Accepted extends ApiInvitationStatus("Accepted")
  case Rejected extends ApiInvitationStatus("Rejected")
  case Cancelled extends ApiInvitationStatus("Cancelled")
  case Deauthorised extends ApiInvitationStatus("Deauthorised")
  case Expired extends ApiInvitationStatus("Expired")
  case PartialAuth extends ApiInvitationStatus("Partialauth")

object ApiInvitationStatus:
  given Format[ApiInvitationStatus] with
    override def reads(json: JsValue): JsResult[ApiInvitationStatus] =
      json.validate[String].flatMap: value =>
        ApiInvitationStatus.values.find(_.value == value) match
          case Some(status) => JsSuccess(status)
          case None         => JsError(s"Unsupported invitation status: $value")

    override def writes(status: ApiInvitationStatus): JsValue = JsString(status.value)

final case class AcrInvitationV3(
  invitationId: String,
  service: String,
  status: ApiInvitationStatus,
  expiryDate: LocalDate,
  created: Instant,
  lastUpdated: Instant
)

object AcrInvitationV3:
  given OFormat[AcrInvitationV3] = Json.format[AcrInvitationV3]

final case class AcrInvitationPageV3(requests: Seq[AcrInvitationV3], totalResults: Int)

object AcrInvitationPageV3:
  given Reads[AcrInvitationPageV3] = Json.reads[AcrInvitationPageV3]

final case class AcrAgentLinkV3(uid: String, normalizedAgentName: String)

object AcrAgentLinkV3:
  given Reads[AcrAgentLinkV3] = Json.reads[AcrAgentLinkV3]

final case class GetInvitationV3Response(
  href: String,
  service: ApiService,
  status: ApiInvitationStatus,
  created: Instant,
  updated: Option[Instant],
  expiresOn: Option[LocalDate],
  clientActionUrl: Option[String],
  agentType: Option[AgentType]
)

object GetInvitationV3Response:
  given OWrites[GetInvitationV3Response] = OWrites: invitation =>
    Json.obj(
      "href"    -> invitation.href,
      "service" -> invitation.service.value,
      "status"  -> invitation.status,
      "created" -> invitation.created
    ) ++ invitation.updated.fold(Json.obj())(value => Json.obj("updated" -> value)) ++
      invitation.expiresOn.fold(Json.obj())(value => Json.obj("expiresOn" -> value)) ++
      invitation.clientActionUrl.fold(Json.obj())(value => Json.obj("clientActionUrl" -> value)) ++
      invitation.agentType.fold(Json.obj())(value => Json.obj("agentType" -> value))
