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

import play.api.Logger
import play.api.libs.json.{JsSuccess, JsValue}
import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.models.Service.{ItsaMain, ItsaSupp, Vat}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateInvitationService @Inject() (
  lockService: MongoLockService,
  acrConnector: AgentClientRelationshipsConnector
) {

  def createInvitation(
    arn: Arn,
    validCreateInvitationRequest: CreateInvitationRequestToAcr
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[ApiErrorResponse, InvitationId]] =
    lockService
      .acquireLock(
        arn = arn.value,
        service = validCreateInvitationRequest.service.internalServiceName,
        clientId = validCreateInvitationRequest.suppliedClientId
      ) {
        acrConnector
          .createInvitation(arn, validCreateInvitationRequest)
      }
      .map {
        case Some(res) => res
        case None      => Left(LockedRequest)
      }

  def validatePayload(payload: Option[JsValue]): Either[ApiErrorResponse, CreateInvitationRequestToAcr] =
    payload match {
      case None =>
        Logger(getClass).warn("The payload could not be parsed as Json")
        Left(InvalidPayload)
      case Some(jsValue) =>
        jsValue.validate[CreateInvitationPayload] match {
          case JsSuccess(CreateInvitationRequestToAcr(value), _) =>
            if (!supportedClientTypes(value.service).contains(value.clientType)) {
              Left(UnsupportedClientType)
            } else if (!validateClientId(value)) {
              if (Nino.isValid(value.suppliedClientId) || Vrn.isValid(value.suppliedClientId)) {
                Left(ClientIdDoesNotMatchService)
              } else Left(ClientIdInvalidFormat)
            } else if (validateKnownFactType(value.service, value.knownFact)) {
              Right(value)
            } else {
              value.service match {
                case ItsaMain | ItsaSupp => Left(PostcodeFormatInvalid)
                case _                   => Left(VatRegDateFormatInvalid)
              }
            }
          case JsSuccess(CreateInvitationPayload(service, _, _, _, _, _), _)
              if !List("MTD-IT", "MTD-VAT").contains(service.head) =>
            Left(UnsupportedService)
          case JsSuccess(CreateInvitationPayload(_, _, _, _, _, Some(agentType)), _)
              if !List("main", "supporting").contains(agentType) =>
            Left(UnsupportedAgentType)
          case other =>
            Logger(getClass).debug(s"The payload is not valid: $other")
            Left(InvalidPayload)
        }
    }

  private val postcodeRegex =
    "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$|BFPO\\s?[0-9]{1,5}$"

  private val supportedClientTypes: Map[Service, Seq[String]] =
    Map(ItsaMain -> Seq("personal"), ItsaSupp -> Seq("personal"), Vat -> Seq("personal", "business"))

  private def validateClientId(agentInvitation: CreateInvitationRequestToAcr): Boolean =
    if (agentInvitation.service == Vat) {
      Vrn.isValid(agentInvitation.suppliedClientId)
    } else {
      Nino.isValid(agentInvitation.suppliedClientId)
    }

  def validateDate(value: String): Boolean =
    if (parseDate(value)) true else false

  private val dateTimeFormat = DateTimeFormatter.ISO_LOCAL_DATE

  def parseDate(date: String): Boolean =
    try {
      dateTimeFormat.parse(date)
      true
    } catch {
      case _: Throwable => false
    }

  private def validateKnownFactType(service: Service, knownFact: String): Boolean =
    service match {
      case ItsaMain | ItsaSupp =>
        knownFact.matches(postcodeRegex)
      case Vat => validateDate(knownFact)
    }

}
