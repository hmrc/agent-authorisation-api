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
import uk.gov.hmrc.agentauthorisation.models.Service.{ItsaMain, ItsaSupp, Vat}

case class ValidCreateInvitationRequest(
  service: Service,
  suppliedClientId: String,
  knownFact: String,
  clientType: ClientType
)
object ValidCreateInvitationRequest {
  def unapply(arg: CreateInvitationPayload): Option[ValidCreateInvitationRequest] =
    arg match {
      case CreateInvitationPayload(List("MTD-VAT"), _, "vrn", _, _, None) =>
        Some(
          ValidCreateInvitationRequest(
            Vat,
            arg.clientId,
            arg.knownFact,
            ClientType.stringToClientType(arg.clientType)
          )
        )
      case CreateInvitationPayload(List("MTD-IT"), _, "ni", _, _, Some("supporting")) =>
        Some(
          ValidCreateInvitationRequest(
            ItsaSupp,
            arg.clientId,
            arg.knownFact,
            ClientType.stringToClientType(arg.clientType)
          )
        )
      case CreateInvitationPayload(List("MTD-IT"), _, "ni", _, _, Some("main")) =>
        Some(
          ValidCreateInvitationRequest(
            ItsaMain,
            arg.clientId,
            arg.knownFact,
            ClientType.stringToClientType(arg.clientType)
          )
        )
      case CreateInvitationPayload(List("MTD-IT"), _, "ni", _, _, None) =>
        Some(
          ValidCreateInvitationRequest(
            ItsaMain,
            arg.clientId,
            arg.knownFact,
            ClientType.stringToClientType(arg.clientType)
          )
        )
      case _ => None
    }

  implicit val writes: Writes[ValidCreateInvitationRequest] = new Writes[ValidCreateInvitationRequest] {
    override def writes(o: ValidCreateInvitationRequest): JsValue =
      Json.obj(
        "service"          -> o.service,
        "suppliedClientId" -> o.suppliedClientId.replaceAll(" ", ""),
        "knownFact"        -> o.knownFact
      )
  }
}
