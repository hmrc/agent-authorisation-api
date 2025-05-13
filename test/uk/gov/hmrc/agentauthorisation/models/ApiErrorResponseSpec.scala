/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.Json
import uk.gov.hmrc.agentauthorisation.support.BaseSpec

class ApiErrorResponseSpec extends BaseSpec {

  "ApiErrorResponse" should {

    "read from JSON when the code is recognised" in {
      Json.obj("code" -> "AGENT_SUSPENDED").as[ApiErrorResponse] shouldBe NoPermissionOnAgency
      Json.obj("code" -> "AGENT_NOT_SUBSCRIBED").as[ApiErrorResponse] shouldBe AgentNotSubscribed
      Json.obj("code" -> "ALREADY_AUTHORISED").as[ApiErrorResponse] shouldBe AlreadyAuthorised
      Json.obj("code" -> "ALREADY_BEING_PROCESSED").as[ApiErrorResponse] shouldBe LockedRequest
      Json.obj("code" -> "CLIENT_ID_DOES_NOT_MATCH_SERVICE").as[ApiErrorResponse] shouldBe ClientIdDoesNotMatchService
      Json.obj("code" -> "CLIENT_ID_FORMAT_INVALID").as[ApiErrorResponse] shouldBe ClientIdInvalidFormat
      Json.obj("code" -> "CLIENT_REGISTRATION_NOT_FOUND").as[ApiErrorResponse] shouldBe ClientRegistrationNotFound
      Json.obj("code" -> "CLIENT_TYPE_NOT_SUPPORTED").as[ApiErrorResponse] shouldBe UnsupportedClientType
      Json.obj("code" -> "INVALID_PAYLOAD").as[ApiErrorResponse] shouldBe InvalidPayload
      Json.obj("code" -> "POSTCODE_FORMAT_INVALID").as[ApiErrorResponse] shouldBe PostcodeFormatInvalid
      Json.obj("code" -> "SERVICE_NOT_SUPPORTED").as[ApiErrorResponse] shouldBe UnsupportedService
      Json.obj("code" -> "UNAUTHORIZED").as[ApiErrorResponse] shouldBe StandardUnauthorised
      Json.obj("code" -> "VAT_REG_DATE_FORMAT_INVALID").as[ApiErrorResponse] shouldBe VatRegDateFormatInvalid
      Json.obj("code" -> "NOT_AN_AGENT").as[ApiErrorResponse] shouldBe NotAnAgent
      Json.obj("code" -> "INTERNAL_SERVER_ERROR").as[ApiErrorResponse] shouldBe StandardInternalServerError
      Json.obj("code" -> "NO_PERMISSION_ON_AGENCY").as[ApiErrorResponse] shouldBe NoPermissionOnAgency
      Json.obj("code" -> "INVITATION_NOT_FOUND").as[ApiErrorResponse] shouldBe InvitationNotFound
    }

    "fail to read from JSON when the code is not recognised" in {
      intercept[IllegalArgumentException](Json.obj("code" -> "BAD_CODE").as[ApiErrorResponse])
    }

    "write to JSON" in {
      NotAnAgent.toJson shouldBe Json.obj("code" -> "NOT_AN_AGENT", "message" -> NotAnAgent.message)
    }

    "convert to result" in {
      val result = StandardInternalServerError.toResult
      result.header.status shouldBe 500
    }
  }
}
