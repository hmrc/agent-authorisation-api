/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.agentauthorisation.models.Service.{Itsa, Vat}

sealed trait Service {
  override def toString: String = this match {
    case Itsa => "HMRC-MTD-IT"
    case Vat  => "HMRC-MTD-VAT"
  }
}

object Service {

  case object Itsa extends Service

  case object Vat extends Service

  private def stringToService: String => Service = {
    case "HMRC-MTD-IT"  => Itsa
    case "HMRC-MTD-VAT" => Vat
    case alien          => throw new Exception(s"Service $alien not supported")
  }

  implicit val reads: Reads[Service] = new Reads[Service] {
    override def reads(json: JsValue): JsResult[Service] = json match {
      case JsString(name) => JsSuccess(stringToService(name))
      case o              => JsError(s"Cannot parse service from $o, must be JsString.")
    }
  }

  implicit val writes: Writes[Service] = new Writes[Service] {
    override def writes(service: Service): JsValue = JsString(service.toString)
  }
}
