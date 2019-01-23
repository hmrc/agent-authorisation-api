/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.functional.syntax._

case class CreateInvitationPayload(
  service: List[String],
  clientType: String,
  clientIdType: String,
  clientId: String,
  knownFact: String)

case class AgentInvitation(
  service: String,
  clientType: String,
  clientIdType: String,
  clientId: String,
  knownFact: String)

object CreateInvitationPayload {

  implicit val reads: Reads[CreateInvitationPayload] = {
    ((JsPath \ "service").read[List[String]] and
      (JsPath \ "clientType").read[String] and
      (JsPath \ "clientIdType").read[String] and
      (JsPath \ "clientId").read[String].map(_.replaceAll(" ", "")) and
      (JsPath \ "knownFact").read[String])((service, clientType, clientIdType, clientId, knownFact) =>
      CreateInvitationPayload(service, clientType, clientIdType, clientId, knownFact))
  }

  implicit val writes: Writes[CreateInvitationPayload] = new Writes[CreateInvitationPayload] {
    override def writes(o: CreateInvitationPayload): JsValue =
      Json.obj(
        "service"      -> o.service,
        "clientType"   -> o.clientType,
        "clientIdType" -> o.clientIdType,
        "clientId"     -> o.clientId.replaceAll(" ", ""),
        "knownFact"    -> o.knownFact)
  }
}

object AgentInvitation {

  implicit val reads: Reads[AgentInvitation] = {
    ((JsPath \ "service").read[String] and
      (JsPath \ "clientType").read[String] and
      (JsPath \ "clientIdType").read[String] and
      (JsPath \ "clientId").read[String].map(_.replaceAll(" ", "")) and
      (JsPath \ "knownFact").read[String])((service, clientType, clientIdType, clientId, knownFact) =>
      AgentInvitation(service, clientType, clientIdType, clientId, knownFact))
  }

  implicit val writes: Writes[AgentInvitation] = new Writes[AgentInvitation] {
    override def writes(o: AgentInvitation): JsValue =
      Json.obj(
        "service"      -> o.service,
        "clientType"   -> o.clientType,
        "clientIdType" -> o.clientIdType,
        "clientId"     -> o.clientId.replaceAll(" ", ""),
        "knownFact"    -> o.knownFact)
  }
}
