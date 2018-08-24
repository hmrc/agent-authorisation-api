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

  def postcodeFormatInvalid(message: String) = BadRequest(toJson(ErrorBody("POSTCODE_FORMAT_INVALID", message)))

  def postcodeRequired(service: String) =
    BadRequest(toJson(ErrorBody("POSTCODE_REQUIRED", s"Postcode is required for service $service")))

  val PostcodeDoesNotMatch = Forbidden(
    toJson(
      ErrorBody(
        "POSTCODE_DOES_NOT_MATCH",
        "The submitted postcode did not match the client's postcode as held by HMRC.")))

}
