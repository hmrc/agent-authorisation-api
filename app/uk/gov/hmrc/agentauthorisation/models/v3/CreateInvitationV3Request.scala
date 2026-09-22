/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.models.v3

import play.api.libs.json.{JsValue, Json, Reads}
import uk.gov.hmrc.agentauthorisation.models.{AgentType, ApiClientId, ApiErrorResponse, ApiService, ClientIdInvalidFormat, InvalidPayload, KnownFactFormatInvalid, UnsupportedAgentType, UnsupportedService}

final case class CreateInvitationV3Request(
  service: String,
  clientId: String,
  knownFact: Option[String],
  agentType: Option[String]
):
  def resolveService: Either[ApiErrorResponse, ApiService] =
    ApiService.from(service).toRight(UnsupportedService)

  def resolveClientId: Either[ApiErrorResponse, ApiClientId] =
    ApiClientId.from(clientId).toRight(ClientIdInvalidFormat)

  def resolveAgentType: Either[ApiErrorResponse, Option[AgentType]] =
    agentType match
      case None => Right(None)
      case Some(value) =>
        AgentType.values.find(_.agentTypeName == value).map(Some(_)).toRight(UnsupportedAgentType)

  def validateKnownFactPresence(resolvedService: ApiService): Either[ApiErrorResponse, Option[String]] =
    Either.cond(
      resolvedService == ApiService.Trusts || knownFact.nonEmpty,
      knownFact,
      InvalidPayload
    )

  def resolveKnownFact: Either[ApiErrorResponse, Option[ApiKnownFact]] =
    knownFact match
      case None        => Right(None)
      case Some(value) => ApiKnownFact.from(value).map(Some(_)).toRight(KnownFactFormatInvalid)

object CreateInvitationV3Request:
  given Reads[CreateInvitationV3Request] = Json.reads[CreateInvitationV3Request]

  def parse(payload: Option[JsValue]): Either[ApiErrorResponse, CreateInvitationV3Request] =
    payload.flatMap(_.asOpt[CreateInvitationV3Request]).toRight(InvalidPayload)
