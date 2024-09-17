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
  val service: List[Service]
  val status: String
}

case class StoredInvitation(
  href: String,
  created: String,
  expiresOn: String,
  updated: String,
  arn: Arn,
  clientType: Option[String],
  service: Service,
  status: String,
  clientActionUrl: Option[String]
)

object StoredInvitation {

  // This is the published format for expiryDate in the API, even though it's a date not a datetime
  // Don't want to change this, since external software might be relying on this format
  //
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnn")

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
          Service(service),
          status,
          clientActionUrl
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
  service: List[Service],
  status: String,
  expiresOn: Option[String],
  clientActionUrl: Option[String],
  updated: Option[String]
) extends Invitation

object PendingOrRespondedInvitation {

  val writesExternalWithAgentType: Writes[PendingOrRespondedInvitation] =
    new Writes[PendingOrRespondedInvitation] {
      override def writes(o: PendingOrRespondedInvitation): JsValue =
        Json.obj(
          "_links"          -> o._links,
          "created"         -> o.created,
          "expiresOn"       -> o.expiresOn,
          "arn"             -> o.arn.value,
          "service"         -> o.service.map(_.externalServiceName),
          "status"          -> o.status,
          "clientActionUrl" -> o.clientActionUrl,
          "updated"         -> o.updated
        ) ++ o.service.headOption.flatMap(_.agentType).fold(Json.obj())(at => Json.obj("agentType" -> at.agentTypeName))
    }

  val writesExternalWithoutAgentType: Writes[PendingOrRespondedInvitation] = new Writes[PendingOrRespondedInvitation] {
    override def writes(o: PendingOrRespondedInvitation): JsValue =
      Json.obj(
        "_links"          -> o._links,
        "created"         -> o.created,
        "expiresOn"       -> o.expiresOn,
        "arn"             -> o.arn.value,
        "service"         -> o.service.map(_.externalServiceName),
        "status"          -> o.status,
        "clientActionUrl" -> o.clientActionUrl,
        "updated"         -> o.updated
      )
  }

  val seqWritesExternalWithoutAgentType: Writes[Seq[PendingOrRespondedInvitation]] =
    Writes.seq[PendingOrRespondedInvitation](writesExternalWithoutAgentType)
}

case class PendingInvitation(
  href: String,
  created: String,
  expiresOn: String,
  arn: Arn,
  service: List[Service],
  status: String,
  clientActionUrl: String
) extends Invitation

object PendingInvitation {

  def unapply(arg: StoredInvitation): Option[PendingInvitation] = arg match {
    case StoredInvitation(href, created, expiredOn, _, arn, _, service, status, clientActionUrl)
        if status == "Pending" =>
      Some(PendingInvitation(href, created, expiredOn, arn, List(service), status, clientActionUrl.getOrElse("")))
    case _ => None
  }

  val writesExternalWithAgentType: Writes[PendingInvitation] = new Writes[PendingInvitation] {

    override def writes(o: PendingInvitation): JsValue =
      Json.obj(
        "_links"          -> Json.obj("self" -> Json.obj("href" -> o.href)),
        "created"         -> o.created,
        "expiresOn"       -> o.expiresOn,
        "arn"             -> o.arn.value,
        "service"         -> o.service.map(_.externalServiceName),
        "status"          -> o.status,
        "clientActionUrl" -> o.clientActionUrl
      ) ++ o.service.headOption.flatMap(_.agentType).fold(Json.obj())(at => Json.obj("agentType" -> at.agentTypeName))
  }

  val writesExternalWithoutAgentType: Writes[PendingInvitation] = new Writes[PendingInvitation] {

    override def writes(o: PendingInvitation): JsValue =
      Json.obj(
        "_links"          -> Json.obj("self" -> Json.obj("href" -> o.href)),
        "created"         -> o.created,
        "expiresOn"       -> o.expiresOn,
        "arn"             -> o.arn.value,
        "service"         -> o.service.map(_.externalServiceName),
        "status"          -> o.status,
        "clientActionUrl" -> o.clientActionUrl
      )
  }
}

case class RespondedInvitation(
  href: String,
  created: String,
  updated: String,
  arn: Arn,
  service: List[Service],
  status: String
) extends Invitation

object RespondedInvitation {

  def unapply(arg: StoredInvitation): Option[RespondedInvitation] = arg match {
    case StoredInvitation(href, created, _, updated, arn, _, service, status, _) if status != "Pending" =>
      Some(RespondedInvitation(href, created, updated, arn, List(service), status))
    case _ => None
  }

  val writesExternalWithAgentType: Writes[RespondedInvitation] = new Writes[RespondedInvitation] {
    override def writes(o: RespondedInvitation): JsValue =
      Json.obj(
        "_links"  -> Json.obj("self" -> Json.obj("href" -> o.href)),
        "created" -> o.created,
        "updated" -> o.updated,
        "arn"     -> o.arn.value,
        "service" -> o.service.map(_.externalServiceName),
        "status"  -> o.status
      ) ++ o.service.headOption.flatMap(_.agentType).fold(Json.obj())(at => Json.obj("agentType" -> at.agentTypeName))
  }

  val writesExternalWithoutAgentType: Writes[RespondedInvitation] = new Writes[RespondedInvitation] {
    override def writes(o: RespondedInvitation): JsValue =
      Json.obj(
        "_links"  -> Json.obj("self" -> Json.obj("href" -> o.href)),
        "created" -> o.created,
        "updated" -> o.updated,
        "arn"     -> o.arn.value,
        "service" -> o.service.map(_.externalServiceName),
        "status"  -> o.status
      )
  }

}
