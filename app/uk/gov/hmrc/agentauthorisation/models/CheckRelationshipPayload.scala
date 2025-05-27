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

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class CheckRelationshipPayload(
  service: List[String],
  clientType: Option[String],
  clientIdType: String,
  clientId: String,
  knownFact: String,
  agentType: Option[String]
)

object CheckRelationshipPayload {

  implicit val reads: Reads[CheckRelationshipPayload] =
    ((JsPath \ "service").read[List[String]] and
      (JsPath \ "clientType").readNullable[String] and
      (JsPath \ "clientIdType").read[String] and
      (JsPath \ "clientId").read[String].map(_.replaceAll(" ", "")) and
      (JsPath \ "knownFact").read[String] and
      (JsPath \ "agentType").readNullable[String])(
      (service, clientType, clientIdType, clientId, knownFact, agentType) =>
        CheckRelationshipPayload(service, clientType, clientIdType, clientId, knownFact, agentType)
    )

  implicit val writes: Writes[CheckRelationshipPayload] = new Writes[CheckRelationshipPayload] {
    override def writes(o: CheckRelationshipPayload): JsValue =
      Json.obj(
        "service"      -> o.service,
        "clientType"   -> o.clientType,
        "clientIdType" -> o.clientIdType,
        "clientId"     -> o.clientId.replaceAll(" ", ""),
        "knownFact"    -> o.knownFact
      )
  }
}
