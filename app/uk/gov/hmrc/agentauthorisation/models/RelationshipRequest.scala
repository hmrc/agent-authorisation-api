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
import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.agentauthorisation.models

case class CheckRelationshipPayload(service: List[String], clientIdType: String, clientId: String, knownFact: String)

object CheckRelationshipPayload {

  implicit val reads: Reads[CheckRelationshipPayload] = {
    ((JsPath \ "service").read[List[String]] and
      (JsPath \ "clientIdType").read[String] and
      (JsPath \ "clientId").read[String].map(_.replaceAll(" ", "")) and
      (JsPath \ "knownFact").read[String])((service, clientIdType, clientId, knownFact) =>
      CheckRelationshipPayload(service, clientIdType, clientId, knownFact))
  }

  implicit val writes: Writes[CheckRelationshipPayload] = new Writes[CheckRelationshipPayload] {
    override def writes(o: CheckRelationshipPayload): JsValue =
      Json.obj(
        "service"      -> o.service,
        "clientIdType" -> o.clientIdType,
        "clientId"     -> o.clientId.replaceAll(" ", ""),
        "knownFact"    -> o.knownFact)
  }
}

case class RelationshipRequest(service: Service, clientIdType: String, clientId: String, knownFact: String)

object RelationshipRequest {
  implicit val reads: Reads[RelationshipRequest] = {
    ((JsPath \ "service").read[Service] and
      (JsPath \ "clientIdType").read[String] and
      (JsPath \ "clientId").read[String].map(_.replaceAll(" ", "")) and
      (JsPath \ "knownFact").read[String])((service, clientIdType, clientId, knownFact) =>
      models.RelationshipRequest(service, clientIdType, clientId, knownFact))
  }

  implicit val writes: Writes[RelationshipRequest] = new Writes[RelationshipRequest] {
    override def writes(o: RelationshipRequest): JsValue =
      Json.obj(
        "service"      -> o.service,
        "clientIdType" -> o.clientIdType,
        "clientId"     -> o.clientId.replaceAll(" ", ""),
        "knownFact"    -> o.knownFact)
  }
}
