/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.controllers.api.errors
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results._

case class ErrorResponse(httpStatusCode: Int, errorCode: String, message: String) {
  lazy val toJson: JsValue = Json.toJson(this)(ErrorResponse.errorResponseWrites)
}

object ErrorResponse {
  implicit val errorResponseWrites: Writes[ErrorResponse] = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message)
  }

  def errorGenericUnauthorised(code: String, message: String): Result =
    Unauthorized(ErrorResponse(401, code, message).toJson)
  def errorUnauthorisedCustomMessage(message: String): Result = errorGenericUnauthorised("UNAUTHORIZED", message)
  val standardUnauthorised: Result =
    errorGenericUnauthorised("UNAUTHORIZED", "Bearer token is missing or not authorized.")

  def errorGenericNotFound(code: String, message: String): Result = NotFound(ErrorResponse(404, code, message).toJson)
  def errorNotFoundCustomMessage(message: String): Result = errorGenericNotFound("NOT_FOUND", message)
  val standardNotFound: Result =
    errorGenericNotFound("NOT_FOUND", "Resource was not found.")

  def errorGenericBadRequest(code: String, message: String): Result =
    BadRequest(ErrorResponse(400, code, message).toJson)
  def errorBadRequestCustomMessage(message: String): Result = errorGenericBadRequest("BAD_REQUEST", message)
  val standardBadRequest: Result =
    errorGenericBadRequest("BAD_REQUEST", "Bad Request")

  def errorGenericAcceptHeaderInvalid(code: String, message: String): Result =
    NotAcceptable(ErrorResponse(406, code, message).toJson)
  def errorAcceptHeaderInvalidCustomMessage(message: String): Result =
    errorGenericAcceptHeaderInvalid("ACCEPT_HEADER_INVALID", message)
  val standardAcceptHeaderInvalid: Result =
    errorGenericAcceptHeaderInvalid("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid.")

  def errorGenericInternalServerError(code: String, message: String): Result =
    InternalServerError(ErrorResponse(500, code, message).toJson)
  def errorInternalServerErrorCustomMessage(message: String): Result =
    errorGenericInternalServerError("INTERNAL_SERVER_ERROR", message)
  val standardInternalServerError: Result =
    errorGenericInternalServerError("INTERNAL_SERVER_ERROR", "Internal server error.")

}
