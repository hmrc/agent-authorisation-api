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

sealed trait Service {
  def agentType: Option[AgentType]
  def externalServiceName: String
  def internalServiceName: String

}

object Service {

  def apply(internalServiceName: String): Service = internalServiceName match {
    case "HMRC-MTD-IT"      => ItsaMain
    case "HMRC-MTD-IT-SUPP" => ItsaSupp
    case "HMRC-MTD-VAT"     => Vat
    case alien              => throw new RuntimeException(s"Unexpected Service has been passed through: $alien")
  }

  case object ItsaMain extends Service {
    override def agentType: Option[AgentType] = Some(AgentType.Main)
    override def internalServiceName: String = "HMRC-MTD-IT"
    override def externalServiceName: String = "MTD-IT"
  }

  case object ItsaSupp extends Service {
    override def agentType: Option[AgentType] = Some(AgentType.Supporting)
    override def internalServiceName: String = "HMRC-MTD-IT-SUPP"
    override def externalServiceName: String = "MTD-IT"
  }

  case object Vat extends Service {
    override def agentType: Option[AgentType] = None
    override def externalServiceName: String = "MTD-VAT"
    override def internalServiceName: String = "HMRC-MTD-VAT"
  }

  implicit val reads: Reads[Service] = new Reads[Service] {
    override def reads(json: JsValue): JsResult[Service] = json match {
      case JsString(name) => JsSuccess(Service(name))
      case o              => JsError(s"Cannot parse service from $o, must be JsString.")
    }
  }

  implicit val writes: Writes[Service] = new Writes[Service] {
    override def writes(service: Service): JsValue = JsString(service.internalServiceName)
  }
}
