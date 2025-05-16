/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.models

import play.api.libs.json._
import uk.gov.hmrc.agentauthorisation.controllers.api.agent.routes
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset}

case class SingleInvitationDetails(
  agentDetails: AgentDetails,
  invitation: InvitationDetails
)
object SingleInvitationDetails {
  implicit val reads: Reads[SingleInvitationDetails] = Reads { json =>
    JsSuccess(SingleInvitationDetails(json.as[AgentDetails], json.as[InvitationDetails]))
  }
  def apiWrites(arn: Arn, acrfUrl: String): Writes[SingleInvitationDetails] = Writes { details =>
    Json.toJson(details.invitation)(InvitationDetails.apiWrites(arn, acrfUrl, details.agentDetails))
  }
}

case class AllInvitationDetails(
  agentDetails: AgentDetails,
  invitations: Seq[InvitationDetails]
)
object AllInvitationDetails {
  implicit val reads: Reads[AllInvitationDetails] = Reads { json =>
    JsSuccess(
      AllInvitationDetails(
        json.as[AgentDetails],
        (json \ "invitations").as[Seq[InvitationDetails]]
      )
    )
  }
  def apiWrites(arn: Arn, acrfUrl: String): Writes[AllInvitationDetails] = Writes { details =>
    implicit val invitationWrites: Writes[InvitationDetails] =
      InvitationDetails.apiWrites(arn, acrfUrl, details.agentDetails)
    Json.toJson(details.invitations)
  }
}

case class AgentDetails(
  uid: String,
  normalizedAgentName: String
)
object AgentDetails {
  implicit val reads: Reads[AgentDetails] = Json.reads[AgentDetails]
}

case class InvitationDetails(
  created: Instant,
  service: Service,
  status: String,
  expiresOn: LocalDate,
  invitationId: String,
  lastUpdated: Instant
)
object InvitationDetails {
  implicit val reads: Reads[InvitationDetails] = Json.reads[InvitationDetails]

  def apiWrites(arn: Arn, acrfUrl: String, agentDetails: AgentDetails): Writes[InvitationDetails] =
    Writes { invitation =>
      lazy val hrefJson: JsObject = Json.obj(
        "self" -> Json.obj(
          "href" -> routes.GetInvitationsController.getInvitationApi(arn, InvitationId(invitation.invitationId)).path()
        )
      )
      lazy val agentTypeJson: JsObject =
        invitation.service.agentType.fold(Json.obj())(agentType => Json.obj("agentType" -> agentType.agentTypeName))
      lazy val clientActionPath = s"$acrfUrl/agent-client-relationships/appoint-someone-to-deal-with-HMRC-for-you" +
        s"/${agentDetails.uid}/${agentDetails.normalizedAgentName}/${invitation.service.urlPart}"

      invitation.status match {
        case "Pending" =>
          Json.obj(
            "_links"          -> hrefJson,
            "created"         -> invitation.created,
            "expiresOn"       -> dateTimeFormatter.format(invitation.expiresOn.atStartOfDay),
            "arn"             -> arn.value,
            "service"         -> Seq(invitation.service.externalServiceName),
            "status"          -> invitation.status,
            "clientActionUrl" -> clientActionPath
          ) ++ agentTypeJson
        case _ =>
          Json.obj(
            "_links"  -> hrefJson,
            "created" -> invitation.created,
            "updated" -> invitation.lastUpdated,
            "arn"     -> arn.value,
            "service" -> Seq(invitation.service.externalServiceName),
            "status"  -> invitation.status
          ) ++ agentTypeJson
      }
    }

  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnn")
    .withZone(ZoneOffset.UTC)

}
