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

import play.api.libs.json.{JsObject, Json, Reads, Writes}
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.controllers.api.agent.routes
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset}

case class InvitationDetails(
  uid: String,
  normalizedAgentName: String,
  created: Instant,
  service: Service,
  status: String,
  expiresOn: LocalDate,
  invitationId: String,
  lastUpdated: Instant
) {
  lazy val clientActionPath = s"/appoint-someone-to-deal-with-HMRC-for-you/$uid/$normalizedAgentName/${service.urlPart}"
}

object InvitationDetails {
  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnn")
    .withZone(ZoneOffset.UTC)

  private def hrefJson(arn: Arn, invitationId: InvitationId): JsObject = Json.obj(
    "self" -> Json.obj(
      "href" -> routes.GetInvitationsController.getInvitationApi(arn, invitationId).path()
    )
  )

  private def agentTypeJson(service: Service): JsObject =
    service.agentType.fold(Json.obj())(agentType => Json.obj("agentType" -> agentType.agentTypeName))

  implicit val reads: Reads[InvitationDetails] = Json.reads[InvitationDetails]

  def writesForStatus(arn: Arn)(implicit appConfig: AppConfig): Writes[InvitationDetails] = Writes { details =>
    details.status match {
      case "Pending" =>
        Json.obj(
          "_links"          -> hrefJson(arn, InvitationId(details.invitationId)),
          "created"         -> details.created,
          "expiresOn"       -> dateTimeFormatter.format(details.expiresOn.atStartOfDay),
          "arn"             -> arn.value,
          "service"         -> Seq(details.service.externalServiceName),
          "status"          -> details.status,
          "clientActionUrl" -> (appConfig.acrfExternalUrl + details.clientActionPath)
        ) ++ agentTypeJson(details.service)
      case _ =>
        Json.obj(
          "_links"  -> hrefJson(arn, InvitationId(details.invitationId)),
          "created" -> details.created,
          "updated" -> details.lastUpdated,
          "arn"     -> arn.value,
          "service" -> Seq(details.service.externalServiceName),
          "status"  -> details.status
        ) ++ agentTypeJson(details.service)
    }
  }
}
