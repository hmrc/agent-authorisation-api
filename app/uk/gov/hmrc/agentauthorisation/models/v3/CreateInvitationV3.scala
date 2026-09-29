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

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, OFormat}
import uk.gov.hmrc.agentauthorisation.models.{AgentType, ApiClientId, ApiService}

final case class ValidatedCreateInvitationV3(
  service: ApiService,
  clientId: ApiClientId,
  knownFact: Option[ApiKnownFact],
  agentType: Option[AgentType]
)

final case class AcrClientDetails(
  name: String,
  status: Option[AcrClientDetails.Status],
  isOverseas: Option[Boolean],
  knownFacts: Seq[String],
  knownFactType: Option[AcrClientDetails.KnownFactType],
  hasPendingInvitation: Boolean,
  hasExistingRelationshipFor: Option[String],
  clientType: Option[ClientType] = None
):
  def containsKnownFact(value: String): Boolean =
    knownFacts.map(AcrClientDetails.normalise).contains(AcrClientDetails.normalise(value))

object AcrClientDetails:
  enum Status(val value: String):
    case Insolvent extends Status("Insolvent")
    case Deregistered extends Status("Deregistered")
    case Inactive extends Status("Inactive")

  object Status:
    given Format[Status] with
      override def reads(json: JsValue): JsResult[Status] =
        json
          .validate[String]
          .flatMap: value =>
            Status.values.find(_.value == value) match
              case Some(status) => JsSuccess(status)
              case None         => JsError(s"Unsupported ACR client status: $value")

      override def writes(status: Status): JsValue = JsString(status.value)

  enum KnownFactType(val value: String):
    case PostalCode extends KnownFactType("PostalCode")
    case CountryCode extends KnownFactType("CountryCode")
    case Country extends KnownFactType("Country")
    case Email extends KnownFactType("Email")
    case Date extends KnownFactType("Date")

  object KnownFactType:
    def fromApi(knownFactType: ApiKnownFactType): KnownFactType =
      knownFactType match
        case ApiKnownFactType.PostalCode  => PostalCode
        case ApiKnownFactType.CountryCode => CountryCode
        case ApiKnownFactType.Email       => Email
        case ApiKnownFactType.Date        => Date

    given Format[KnownFactType] with
      override def reads(json: JsValue): JsResult[KnownFactType] =
        json
          .validate[String]
          .flatMap: value =>
            KnownFactType.values.find(_.value == value) match
              case Some(knownFactType) => JsSuccess(knownFactType)
              case None                => JsError(s"Unsupported ACR known fact type: $value")

      override def writes(knownFactType: KnownFactType): JsValue = JsString(knownFactType.value)

  private def normalise(value: String): String = value.replaceAll("\\s", "").toUpperCase

  given OFormat[AcrClientDetails] = Json.format[AcrClientDetails]

final case class AcrCreateInvitationRequest(
  clientId: String,
  suppliedClientIdType: String,
  clientName: String,
  service: String,
  clientType: Option[ClientType] = None
)

object AcrCreateInvitationRequest:
  given OFormat[AcrCreateInvitationRequest] = Json.format[AcrCreateInvitationRequest]

final case class CreateInvitationV3Response(invitationId: String)

object CreateInvitationV3Response:
  given OFormat[CreateInvitationV3Response] = Json.format[CreateInvitationV3Response]
