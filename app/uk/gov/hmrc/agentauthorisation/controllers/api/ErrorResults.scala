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
        "Received an invalid service")))

  val InvalidItsaNino = BadRequest(
    toJson(
      ErrorBody(
        "CLIENT_ID_FORMAT_INVALID",
        "Received an invalid nino")))

  val PostcodeRequired = BadRequest(
    toJson(
      ErrorBody(
        "POSTCODE_REQUIRED",
        s"Postcode is required for MTD-IT")))

  val PostcodeFormatInvalid = BadRequest(
    toJson(
      ErrorBody(
        "POSTCODE_FORMAT_INVALID",
        "The postcode is an invalid format")))

  val PostcodeDoesNotMatch = Forbidden(
    toJson(
      ErrorBody(
        "POSTCODE_DOES_NOT_MATCH",
        "The submitted postcode did not match the client's postcode as held by HMRC.")))

  val ClientRegistrationNotFound = Forbidden(
    toJson(
      ErrorBody(
        "CLIENT_REGISTRATION_NOT_FOUND",
        "Client is not subscribed to MTD-IT")))

  val NoPermissionOnAgency = Forbidden(
    toJson(
      ErrorBody(
        "NO_PERMISSION_ON_AGENCY",
        "The logged in user is not permitted to access invitations for the specified agency.")))

  val NotAnAgent = Forbidden(
    toJson(
      ErrorBody(
        "NOT_AN_AGENT",
        "The logged in user is not an agent.")))

}
