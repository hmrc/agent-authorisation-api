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

package uk.gov.hmrc.agentauthorisation.controllers.api

import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results._

object ErrorResults {

  case class ErrorBody(code: String, message: String)

  implicit val errorBodyWrites: Writes[ErrorBody] = new Writes[ErrorBody] {
    override def writes(body: ErrorBody): JsValue =
      Json.obj("code" -> body.code, "message" -> body.message)
  }

  val UnsupportedServiceResult: Result = BadRequest(
    toJson(
      ErrorBody(
        "SERVICE_NOT_SUPPORTED",
        "The service requested is not supported. Check the API documentation to find which services are supported."
      )
    )
  )

  val UnsupportedClientTypeResult: Result = BadRequest(
    toJson(
      ErrorBody(
        "CLIENT_TYPE_NOT_SUPPORTED",
        "The client type requested is not supported. Check the API documentation to find which client types are supported."
      )
    )
  )

  val UnsupportedAgentTypeResult: Result = BadRequest(
    toJson(
      ErrorBody(
        "AGENT_TYPE_NOT_SUPPORTED",
        "The agent type requested is not supported. Check the API documentation to find which agent types are supported."
      )
    )
  )

  val ClientIdDoesNotMatchServiceResult: Result = BadRequest(
    toJson(
      ErrorBody(
        "CLIENT_ID_DOES_NOT_MATCH_SERVICE",
        "The specified client identifier does not match the requested service. Check the API documentation to find the correct format."
      )
    )
  )

  val ClientIdInvalidFormatResult: Result = BadRequest(
    toJson(
      ErrorBody(
        "CLIENT_ID_FORMAT_INVALID",
        "Client identifier must be in the correct format. Check the API documentation to find the correct format."
      )
    )
  )

  val PostcodeFormatInvalidResult: Result = BadRequest(
    toJson(
      ErrorBody(
        "POSTCODE_FORMAT_INVALID",
        "Postcode must be in the correct format. Check the API documentation to find the correct format."
      )
    )
  )

  val VatRegDateFormatInvalidResult: Result = BadRequest(
    toJson(
      ErrorBody(
        "VAT_REG_DATE_FORMAT_INVALID",
        "VAT registration date must be in the correct format. Check the API documentation to find the correct format."
      )
    )
  )

  val LockedRequestResult: Result = Forbidden(
    toJson(ErrorBody("ALREADY_BEING_PROCESSED", "More than one request received to create the same invitation."))
  )

  val PostcodeDoesNotMatchResult: Result = Forbidden(
    toJson(ErrorBody("POSTCODE_DOES_NOT_MATCH", "The postcode provided does not match HMRC's record for this client."))
  )

  val VatRegDateDoesNotMatchResultResult: Result = Forbidden(
    toJson(
      ErrorBody(
        "VAT_REG_DATE_DOES_NOT_MATCH",
        "The VAT registration date provided does not match HMRC's record for this client."
      )
    )
  )

  val VatClientInsolventResult: Result = Forbidden(
    toJson(
      ErrorBody(
        "VAT_CLIENT_INSOLVENT",
        "The Vat registration number belongs to a customer that is insolvent."
      )
    )
  )

  val ClientRegistrationNotFoundResult: Result = Forbidden(
    toJson(
      ErrorBody("CLIENT_REGISTRATION_NOT_FOUND", "The details provided for this client do not match HMRC's records.")
    )
  )

  val NoPermissionOnAgencyResult: Result = Forbidden(
    toJson(
      ErrorBody(
        "NO_PERMISSION_ON_AGENCY",
        "The user that is signed in cannot access this authorisation request. Their details do not match the agent business that created the authorisation request."
      )
    )
  )

  val NotAnAgentResult: Result = Forbidden(
    toJson(
      ErrorBody(
        "NOT_AN_AGENT",
        "This user does not have a Government Gateway agent account. They need to create an Government Gateway agent account before they can use this service."
      )
    )
  )

  val AgentNotSubscribedResult: Result = Forbidden(
    toJson(
      ErrorBody(
        "AGENT_NOT_SUBSCRIBED",
        "This agent needs to create an agent services account before they can use this service."
      )
    )
  )

  val InvitationNotFoundResult: Result = NotFound(
    toJson(ErrorBody("INVITATION_NOT_FOUND", "The authorisation request cannot be found."))
  )

  val InvalidInvitationStatusResult: Result = Forbidden(
    toJson(
      ErrorBody(
        "INVALID_INVITATION_STATUS",
        "This authorisation request cannot be cancelled as the client has already responded to the request, or the request has expired."
      )
    )
  )

  val RelationshipNotFoundResult: Result = NotFound(
    toJson(
      ErrorBody("RELATIONSHIP_NOT_FOUND", "Relationship is inactive. Agent is not authorised to act for this client.")
    )
  )

  val InvalidPayloadResult: Result = BadRequest(toJson(ErrorBody("INVALID_PAYLOAD", "The payload is invalid.")))

  val DuplicateAuthorisationRequestResult: Result = Forbidden(
    toJson(
      ErrorBody(
        "DUPLICATE_AUTHORISATION_REQUEST",
        "An authorisation request for this service has already been created and is awaiting the client’s response."
      )
    )
  )

  val AlreadyAuthorisedResult: Result = Forbidden(
    toJson(
      ErrorBody(
        "ALREADY_AUTHORISED",
        "The client has already authorised the agent for this service. The agent does not need ask the client for this authorisation again."
      )
    )
  )
}
