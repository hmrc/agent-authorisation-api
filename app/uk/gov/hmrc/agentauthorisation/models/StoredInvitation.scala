/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter
import play.api.libs.json._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import play.api.libs.functional.syntax._

trait Invitation {
  val service: List[String]
  val status: String
}

case class StoredInvitation(
  href: String,
  created: String,
  expiresOn: String,
  updated: String,
  arn: Arn,
  clientType: Option[String],
  service: String,
  status: String,
  clientActionUrl: Option[String],
  agentType: Option[AgentType]
)

object StoredInvitation {

  val transformService: String => String = {
    case "HMRC-MTD-IT"      => "MTD-IT"
    case "HMRC-MTD-IT-SUPP" => "MTD-IT"
    case "HMRC-MTD-VAT"     => "MTD-VAT"
    case e                  => throw new RuntimeException(s"Unexpected Service has been passed through: $e")
  }

  val getAgentType: String => Option[AgentType] = {
    case "HMRC-MTD-IT"      => Some(AgentType.Main)
    case "HMRC-MTD-IT-SUPP" => Some(AgentType.Supporting)
    case _                  => None
  }
  // This is the published format for expiryDate in the API, even though it's a date not a datetime
  // Don't want to change this, since external software might be relying on this format
  //
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnn")

  // TODO WG - here code logic for agentType
  implicit val reads: Reads[StoredInvitation] =
    ((JsPath \ "_links" \ "self" \ "href").read[String] and
      (JsPath \ "created").read[String] and
      (JsPath \ "expiryDate").read[String].map(LocalDate.parse) and
      (JsPath \ "lastUpdated").read[String] and
      (JsPath \ "arn").read[Arn] and
      (JsPath \ "clientType").readNullable[String] and
      (JsPath \ "service").read[String] and
      (JsPath \ "status").read[String] and
      (JsPath \ "clientActionUrl")
        .readNullable[String])(
      (selfLink, created, expiresOn, updated, arn, clientType, service, status, clientActionUrl) =>
        StoredInvitation(
          selfLink,
          created,
          LocalDateTime.of(expiresOn, LocalTime.MIDNIGHT).format(dateTimeFormatter),
          updated,
          arn,
          clientType,
          transformService(service),
          status,
          clientActionUrl,
          getAgentType(service)
        )
    )
}

case class Links(self: Option[Href])

object Links {

  def apply(self: String): Links = Links(self = Some(Href(self)))

  implicit val format: OFormat[Links] = Json.format[Links]
}

case class Href(href: String)

object Href {
  implicit val format: OFormat[Href] = Json.format[Href]
}

case class PendingOrRespondedInvitation(
  _links: Links,
  created: String,
  arn: Arn,
  service: List[String],
  status: String,
  expiresOn: Option[String],
  clientActionUrl: Option[String],
  updated: Option[String],
  agentType: Option[AgentType]
) extends Invitation

object PendingOrRespondedInvitation {

  implicit val reads: Reads[PendingOrRespondedInvitation] = Json.reads[PendingOrRespondedInvitation]

  implicit val writes: Writes[PendingOrRespondedInvitation] = Json.writes[PendingOrRespondedInvitation]
}

case class PendingInvitation(
  href: String,
  created: String,
  expiresOn: String,
  arn: Arn,
  service: List[String],
  status: String,
  clientActionUrl: String,
  agentType: Option[AgentType]
) extends Invitation

object PendingInvitation {

  def unapply(arg: StoredInvitation): Option[PendingInvitation] = arg match {
    case StoredInvitation(href, created, expiredOn, _, arn, _, service, status, clientActionUrl, agentType)
        if status == "Pending" =>
      Some(
        PendingInvitation(
          href,
          created,
          expiredOn,
          arn,
          List(service),
          status,
          clientActionUrl.getOrElse(""),
          agentType
        )
      )
    case _ => None
  }

  implicit val reads: Reads[PendingInvitation] =
    ((JsPath \ "_links" \ "self" \ "href").read[String] and
      (JsPath \ "created").read[String] and
      (JsPath \ "expiresOn").read[String] and
      (JsPath \ "arn").read[Arn] and
      (JsPath \ "service").read[String] and
      (JsPath \ "status").read[String] and
      (JsPath \ "clientActionUrl").read[String] and
      (JsPath \ "agentType").readNullable[AgentType])(
      (selfLink, created, expiresOn, arn, service, status, clientActionUrl, agentType) =>
        PendingInvitation(selfLink, created, expiresOn, arn, List(service), status, clientActionUrl, agentType)
    )

  implicit val writes: Writes[PendingInvitation] = new Writes[PendingInvitation] {
    override def writes(o: PendingInvitation): JsValue =
      Json.obj(
        "_links"          -> Json.obj("self" -> Json.obj("href" -> o.href)),
        "created"         -> o.created,
        "expiresOn"       -> o.expiresOn,
        "arn"             -> o.arn.value,
        "service"         -> o.service,
        "status"          -> o.status,
        "clientActionUrl" -> o.clientActionUrl
      ) ++ o.agentType.fold(Json.obj())(at => Json.obj("agentType" -> at.toString))
  }
}

case class RespondedInvitation(
  href: String,
  created: String,
  updated: String,
  arn: Arn,
  service: List[String],
  status: String,
  agentType: Option[AgentType]
) extends Invitation

object RespondedInvitation {

  def unapply(arg: StoredInvitation): Option[RespondedInvitation] = arg match {
    case StoredInvitation(href, created, _, updated, arn, _, service, status, _, agentType) if status != "Pending" =>
      Some(RespondedInvitation(href, created, updated, arn, List(service), status, agentType))
    case _ => None
  }

  implicit val read: Reads[RespondedInvitation] =
    ((JsPath \ "_links" \ "self" \ "href").read[String] and
      (JsPath \ "created").read[String] and
      (JsPath \ "updated").read[String] and
      (JsPath \ "arn").read[Arn] and
      (JsPath \ "service").read[String] and
      (JsPath \ "status").read[String] and
      (JsPath \ "agentType").readNullable[AgentType])((href, created, updated, arn, service, status, agentType) =>
      RespondedInvitation(href, created, updated, arn, List(service), status, agentType)
    )

  implicit val writes: Writes[RespondedInvitation] = new Writes[RespondedInvitation] {
    override def writes(o: RespondedInvitation): JsValue =
      Json.obj(
        "_links"  -> Json.obj("self" -> Json.obj("href" -> o.href)),
        "created" -> o.created,
        "updated" -> o.updated,
        "arn"     -> o.arn.value,
        "service" -> o.service,
        "status"  -> o.status
      ) ++ o.agentType.fold(Json.obj())(at => Json.obj("agentType" -> at.toString))
  }
}
