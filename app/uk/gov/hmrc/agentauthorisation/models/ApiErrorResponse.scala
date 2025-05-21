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

package uk.gov.hmrc.agentauthorisation.models

import play.api.Logging
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.agentmtdidentifiers.model.InvitationId
import uk.gov.hmrc.agentauthorisation.models.Service.{ItsaMain, ItsaSupp, Vat}

abstract class ApiErrorResponse(val statusCode: Int, val code: String, val message: String) extends Logging {
  def toResult: Result = {
    logger.warn(s"Create invitation error ${this.code}: ${this.message}")
    this.statusCode match {
      case 400 =>
        BadRequest(this.toJson)
      case 401 =>
        Unauthorized(this.toJson)
      case 403 =>
        Forbidden(this.toJson)
      case 404 =>
        NotFound(this.toJson)
      case 406 =>
        NotAcceptable(this.toJson)
      case _ =>
        InternalServerError(this.toJson)
    }
  }
  lazy val toJson: JsValue = Json.toJson(this)(ApiErrorResponse.errorResponseWrites)
}

object ApiErrorResponse {
  def acrReads(service: Option[Service] = None): Reads[ApiErrorResponse] =
    Reads { json =>
      val response = (json \ "code").as[String] match {
        case "AGENT_SUSPENDED"                  => NoPermissionOnAgency
        case "AGENT_NOT_SUBSCRIBED"             => AgentNotSubscribed
        case "ALREADY_AUTHORISED"               => AlreadyAuthorised
        case "ALREADY_BEING_PROCESSED"          => LockedRequest
        case "CLIENT_ID_DOES_NOT_MATCH_SERVICE" => ClientIdDoesNotMatchService
        case "CLIENT_ID_FORMAT_INVALID"         => ClientIdInvalidFormat
        case "CLIENT_REGISTRATION_NOT_FOUND"    => ClientRegistrationNotFound
        case "CLIENT_TYPE_NOT_SUPPORTED"        => UnsupportedClientType
        case "DUPLICATE_AUTHORISATION_REQUEST" =>
          DuplicateAuthorisationRequest(InvitationId((json \ "invitationId").as[String]))
        case "INTERNAL_SERVER_ERROR"       => StandardInternalServerError
        case "INVALID_INVITATION_STATUS"   => InvalidInvitationStatus
        case "INVALID_PAYLOAD"             => InvalidPayload
        case "NOT_AN_AGENT"                => NotAnAgent
        case "POSTCODE_DOES_NOT_MATCH"     => PostcodeDoesNotMatch
        case "POSTCODE_FORMAT_INVALID"     => PostcodeFormatInvalid
        case "RELATIONSHIP_NOT_FOUND"      => RelationshipNotFound
        case "SERVICE_NOT_SUPPORTED"       => UnsupportedService
        case "VAT_CLIENT_INSOLVENT"        => VatClientInsolvent
        case "VAT_REG_DATE_DOES_NOT_MATCH" => VatRegDateDoesNotMatch
        case "VAT_REG_DATE_FORMAT_INVALID" => VatRegDateFormatInvalid
        case "UNAUTHORIZED"                => StandardUnauthorised
        case "NO_PERMISSION_ON_AGENCY"     => NoPermissionOnAgency
        case "INVITATION_NOT_FOUND"        => InvitationNotFound
        case "KNOWN_FACT_DOES_NOT_MATCH" =>
          service match {
            case Some(ItsaMain) | Some(ItsaSupp) => PostcodeDoesNotMatch
            case Some(Vat)                       => VatRegDateDoesNotMatch
            case None                            => UnsupportedService
          }
        case "CLIENT_INSOLVENT" => VatClientInsolvent
        case value              => throw new IllegalArgumentException(s"Unexpected error code: $value")
      }
      JsSuccess(response)
    }

  implicit val errorResponseWrites: Writes[ApiErrorResponse] = new Writes[ApiErrorResponse] {
    def writes(e: ApiErrorResponse): JsValue = Json.obj("code" -> e.code, "message" -> e.message)
  }
}

case object AgentNotSubscribed
    extends ApiErrorResponse(
      403,
      "AGENT_NOT_SUBSCRIBED",
      "This agent needs to create an agent services account before they can use this service."
    )

case object AlreadyAuthorised
    extends ApiErrorResponse(
      403,
      "ALREADY_AUTHORISED",
      "The client has already authorised the agent for this service. The agent does not need ask the client for this authorisation again."
    )

case object ClientIdDoesNotMatchService
    extends ApiErrorResponse(
      400,
      "CLIENT_ID_DOES_NOT_MATCH_SERVICE",
      "The specified client identifier does not match the requested service. Check the API documentation to find the correct format."
    )

case object ClientIdInvalidFormat
    extends ApiErrorResponse(
      400,
      "CLIENT_ID_FORMAT_INVALID",
      "Client identifier must be in the correct format. Check the API documentation to find the correct format."
    )

case object ClientRegistrationNotFound
    extends ApiErrorResponse(
      403,
      "CLIENT_REGISTRATION_NOT_FOUND",
      "The details provided for this client do not match HMRC's records."
    )

case class DuplicateAuthorisationRequest(invitationId: InvitationId)
    extends ApiErrorResponse(
      statusCode = 403,
      code = "DUPLICATE_AUTHORISATION_REQUEST",
      message = "The authorisation request is a duplicate of a previous request."
    )

case object InvalidAcceptHeader
    extends ApiErrorResponse(
      406,
      "ACCEPT_HEADER_INVALID",
      "Invalid 'Accept' header."
    )

case object InvalidInvitationStatus
    extends ApiErrorResponse(
      403,
      "INVALID_INVITATION_STATUS",
      "This authorisation request cannot be cancelled as the client has already responded to the request, or the request has expired."
    )

case object InvalidPayload
    extends ApiErrorResponse(
      400,
      "INVALID_PAYLOAD",
      "The payload is invalid."
    )

case object InvitationNotFound
    extends ApiErrorResponse(
      404,
      "INVITATION_NOT_FOUND",
      "The authorisation request cannot be found."
    )

case object LockedRequest
    extends ApiErrorResponse(
      403,
      "ALREADY_BEING_PROCESSED",
      "More than one request received to create the same invitation."
    )

case object MissingAcceptHeader
    extends ApiErrorResponse(
      406,
      "ACCEPT_HEADER_INVALID",
      "Missing 'Accept' header."
    )

case object MissingOrUnsupportedContentType
    extends ApiErrorResponse(
      400,
      "BAD_REQUEST",
      "Missing or unsupported content-type."
    )

case object MissingOrUnsupportedVersion
    extends ApiErrorResponse(
      400,
      "BAD_REQUEST",
      "Missing or unsupported version number."
    )

case object NoPermissionOnAgency
    extends ApiErrorResponse(
      403,
      "NO_PERMISSION_ON_AGENCY",
      "The user that is signed in cannot access this authorisation request. Their details do not match the agent " +
        "business that created the authorisation request."
    )

case object NotAnAgent
    extends ApiErrorResponse(
      403,
      "NOT_AN_AGENT",
      "This user does not have a Government Gateway agent account. They need to create an Government Gateway agent account before they can use this service."
    )

case object PostcodeDoesNotMatch
    extends ApiErrorResponse(
      403,
      "POSTCODE_DOES_NOT_MATCH",
      "The postcode provided does not match HMRC's record for this client."
    )

case object PostcodeFormatInvalid
    extends ApiErrorResponse(
      400,
      "POSTCODE_FORMAT_INVALID",
      "Postcode must be in the correct format. Check the API documentation to find the correct format."
    )

case object RelationshipNotFound
    extends ApiErrorResponse(
      404,
      "RELATIONSHIP_NOT_FOUND",
      "Relationship is inactive. Agent is not authorised to act for this client."
    )

case object StandardBadRequest
    extends ApiErrorResponse(
      400,
      "BAD_REQUEST",
      "Bad Request"
    )

case object StandardInternalServerError
    extends ApiErrorResponse(
      500,
      "INTERNAL_SERVER_ERROR",
      "Internal server error."
    )

case object StandardNotFound
    extends ApiErrorResponse(
      404,
      "NOT_FOUND",
      "Resource was not found."
    )

case object StandardUnauthorised
    extends ApiErrorResponse(
      401,
      "UNAUTHORIZED",
      "Bearer token is missing or not authorized."
    )

case object UnsupportedAgentType
    extends ApiErrorResponse(
      400,
      "AGENT_TYPE_NOT_SUPPORTED",
      "The agent type requested is not supported. Check the API documentation to find which agent types are supported."
    )

case object UnsupportedClientType
    extends ApiErrorResponse(
      400,
      "CLIENT_TYPE_NOT_SUPPORTED",
      "The client type requested is not supported. Check the API documentation to find which client types are supported."
    )

case object UnsupportedService
    extends ApiErrorResponse(
      400,
      "SERVICE_NOT_SUPPORTED",
      "The service requested is not supported. Check the API documentation to find which services are supported."
    )

case object VatClientInsolvent
    extends ApiErrorResponse(
      403,
      "VAT_CLIENT_INSOLVENT",
      "The Vat registration number belongs to a customer that is insolvent."
    )

case object VatRegDateDoesNotMatch
    extends ApiErrorResponse(
      403,
      "VAT_REG_DATE_DOES_NOT_MATCH",
      "The VAT registration date provided does not match HMRC's record for this client."
    )

case object VatRegDateFormatInvalid
    extends ApiErrorResponse(
      400,
      "VAT_REG_DATE_FORMAT_INVALID",
      "VAT registration date must be in the correct format. Check the API documentation to find the correct format."
    )
