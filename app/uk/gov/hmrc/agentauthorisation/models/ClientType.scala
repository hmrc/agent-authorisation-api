/*
 * Copyright 2020 HM Revenue & Customs
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

sealed trait ClientType

object ClientType {

  case object personal extends ClientType
  case object business extends ClientType

  def stringToClientType: String => ClientType = {
    case "personal" => personal
    case "business" => business
    case alien      => throw new Exception(s"Client type $alien not supported")
  }

  implicit val reads: Reads[ClientType] = new Reads[ClientType] {
    override def reads(json: JsValue): JsResult[ClientType] = json match {
      case JsString(name) => JsSuccess(stringToClientType(name))
      case o              => JsError(s"Cannot parse client type from $o, must be JsString.")
    }
  }

  implicit val writes: Writes[ClientType] = new Writes[ClientType] {
    override def writes(clientType: ClientType): JsValue = JsString(clientType.toString)
  }
}
