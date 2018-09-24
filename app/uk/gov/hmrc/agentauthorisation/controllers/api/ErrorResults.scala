/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.libs.json.{ JsValue, Json, Writes }
import play.api.mvc.Results._

object ErrorResults {

  case class ErrorBody(code: String, message: String)

  implicit val errorBodyWrites = new Writes[ErrorBody] {
    override def writes(body: ErrorBody): JsValue = Json.obj("code" -> body.code, "message" -> body.message)
  }

  val UnsupportedService = BadRequest(
    toJson(
      ErrorBody(
        "SERVICE_NOT_SUPPORTED",
        "The service requested is not supported. Check the API documentation to find which services are supported.")))

  val ClientIdDoesNotMatchService = BadRequest(
    toJson(
      ErrorBody(
        "CLIENT_ID_DOES_NOT_MATCH_SERVICE",
        "The specified client identifier does not match the requested service. Check the API documentation to find the correct format.")))

  val ClientIdInvalidFormat = BadRequest(
    toJson(
      ErrorBody(
        "CLIENT_ID_FORMAT_INVALID",
        "Client identifier must be in the correct format. Check the API documentation to find the correct format.")))

  val PostcodeFormatInvalid = BadRequest(
    toJson(
      ErrorBody(
        "POSTCODE_FORMAT_INVALID",
        "Postcode must be in the correct format. Check the API documentation to find the correct format.")))

  val VatRegDateFormatInvalid = BadRequest(
    toJson(
      ErrorBody(
        "VAT_REG_DATE_FORMAT_INVALID",
        "VAT registration date must be in the correct format. Check the API documentation to find the correct format.")))

  val PostcodeDoesNotMatch = Forbidden(
    toJson(
      ErrorBody(
        "POSTCODE_DOES_NOT_MATCH",
        "The postcode provided does not match HMRC's record for this client.")))

  val VatRegDateDoesNotMatch = Forbidden(
    toJson(
      ErrorBody(
        "VAT_REG_DATE_DOES_NOT_MATCH",
        "The VAT registration date provided does not match HMRC's record for this client.")))

  val ClientRegistrationNotFound = Forbidden(
    toJson(
      ErrorBody(
        "CLIENT_REGISTRATION_NOT_FOUND",
        "The details provided for this client do not match HMRC's records.")))

  val NoPermissionOnAgency = Forbidden(
    toJson(
      ErrorBody(
        "NO_PERMISSION_ON_AGENCY",
        "The account used to sign in cannot access this authorisation request. Their details do not match the agent business that created the authorisation request.")))

  val NotAnAgent = Forbidden(
    toJson(
      ErrorBody(
        "NOT_AN_AGENT",
        "This account used to sign in is not an Government Gateway account for an agent. The agent needs to create an Government Gateway agent account before they can use this service.")))

  val AgentNotSubscribed = Forbidden(
    toJson(
      ErrorBody(
        "AGENT_NOT_SUBSCRIBED",
        "This agent needs to create an agent services account before they can use this service.")))

  val InvitationNotFound = NotFound(
    toJson(
      ErrorBody(
        "INVITATION_NOT_FOUND",
        "The authorisation request cannot be found.")))

  val InvalidInvitationStatus = Forbidden(
    toJson(
      ErrorBody(
        "INVALID_INVITATION_STATUS",
        "This authorisation request cannot be cancelled as the client has already responded to the request, or the request has expired.")))

  val RelationshipNotFound = NotFound(
    toJson(
      ErrorBody(
        "RELATIONSHIP_NOT_FOUND",
        "Relationship is inactive. Agent is not authorised to act for this client.")))
}
