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

package uk.gov.hmrc.agentauthorisation.services

import play.api.Logging
import play.api.libs.json.{JsSuccess, JsValue}
import uk.gov.hmrc.agentauthorisation.models.Service.{ItsaMain, ItsaSupp, Vat}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentmtdidentifiers.model.Vrn
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
class ValidateClientAccessDataService @Inject() () extends Logging {

  def validatePayload(payload: Option[JsValue]): Either[ApiErrorResponse, ClientAccessData] =
    payload match {
      case None =>
        logger.warn("The payload could not be parsed as Json")
        Left(InvalidPayload)
      case Some(jsValue) =>
        jsValue.validate[CreateInvitationPayload] match {
          case JsSuccess(ClientAccessData(value), _) =>
            validateClientAccessData(value)
          case JsSuccess(CreateInvitationPayload(service, _, _, _, _, _), _)
              if !List("MTD-IT", "MTD-VAT").contains(service.head) =>
            Left(UnsupportedService)
          case JsSuccess(CreateInvitationPayload(_, _, _, _, _, Some(agentType)), _)
              if !List("main", "supporting").contains(agentType) =>
            Left(UnsupportedAgentType)
          case other =>
            logger.debug(s"The payload is not valid: $other")
            Left(InvalidPayload)
        }
    }

  private def validateClientAccessData(
    value: ClientAccessData
  ): Either[ApiErrorResponse, ClientAccessData] =
    if (!supportedClientTypes(value.service).contains(value.clientType)) {
      Left(UnsupportedClientType)
    } else if (!isValidClientId(value)) {
      if (Nino.isValid(value.suppliedClientId) || Vrn.isValid(value.suppliedClientId)) {
        Left(ClientIdDoesNotMatchService)
      } else Left(ClientIdInvalidFormat)
    } else if (isKnownFactTypeValid(value.service, value.knownFact)) {
      Right(value)
    } else {
      value.service match {
        case ItsaMain | ItsaSupp => Left(PostcodeFormatInvalid)
        case _                   => Left(VatRegDateFormatInvalid)
      }
    }

  private val postcodeRegex =
    "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$|BFPO\\s?[0-9]{1,5}$"

  private val personalClientType = "personal"
  private val businessClientType = "business"

  private val supportedClientTypes: Map[Service, Seq[String]] =
    Map(
      ItsaMain -> Seq(personalClientType),
      ItsaSupp -> Seq(personalClientType),
      Vat      -> Seq(personalClientType, businessClientType)
    )

  private def isValidClientId(agentInvitation: ClientAccessData): Boolean =
    if (agentInvitation.service == Vat) {
      Vrn.isValid(agentInvitation.suppliedClientId)
    } else {
      Nino.isValid(agentInvitation.suppliedClientId)
    }

  def isValidDateString(dateString: String): Boolean = Try(
    LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
  ).isSuccess

  private def isKnownFactTypeValid(service: Service, knownFact: String): Boolean =
    service match {
      case ItsaMain | ItsaSupp =>
        knownFact.matches(postcodeRegex)
      case Vat => isValidDateString(knownFact)
    }

}
