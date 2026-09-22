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

import play.api.libs.json.{JsError, JsString, JsSuccess, Json}
import org.scalatest.EitherValues
import uk.gov.hmrc.agentauthorisation.models.{AgentType, ApiClientId, ApiService, ClientIdInvalidFormat, InvalidPayload, KnownFactFormatInvalid, UnsupportedAgentType, UnsupportedService}
import uk.gov.hmrc.agentauthorisation.support.UnitSpec

class CreateInvitationV3RequestSpec extends UnitSpec with EitherValues:

  "CreateInvitationV3Request JSON" should:
    "read all supplied fields" in:
      val json = Json.obj(
        "service" -> "MTD-IT",
        "clientId" -> "AB123456A",
        "knownFact" -> "AA1 1AA",
        "agentType" -> "main"
      )

      json.validate[CreateInvitationV3Request] shouldBe
        JsSuccess(CreateInvitationV3Request("MTD-IT", "AB123456A", Some("AA1 1AA"), Some("main")))

    "allow omitted optional fields for trusts" in:
      val json = Json.obj("service" -> "TRUSTS", "clientId" -> "1234567890")

      json.validate[CreateInvitationV3Request] shouldBe
        JsSuccess(CreateInvitationV3Request("TRUSTS", "1234567890", None, None))

    "reject missing required fields" in:
      Json.obj("clientId" -> "AB123456A").validate[CreateInvitationV3Request] shouldBe a[JsError]
      Json.obj("service" -> "MTD-IT").validate[CreateInvitationV3Request] shouldBe a[JsError]

    "reject non-string field values" in:
      val json = Json.obj("service" -> "MTD-IT", "clientId" -> 123, "knownFact" -> true)

      json.validate[CreateInvitationV3Request] shouldBe a[JsError]

  "CreateInvitationV3Request.parse" should:
    "return the parsed request for structurally valid JSON" in:
      val json = Json.obj("service" -> "TRUSTS", "clientId" -> "1234567890")

      CreateInvitationV3Request.parse(Some(json)) shouldBe
        Right(CreateInvitationV3Request("TRUSTS", "1234567890", None, None))

    "return INVALID_PAYLOAD when the body is absent or not a JSON object" in:
      CreateInvitationV3Request.parse(None) shouldBe Left(InvalidPayload)
      CreateInvitationV3Request.parse(Some(JsString("not an object"))) shouldBe Left(InvalidPayload)

    "return INVALID_PAYLOAD for missing or wrongly typed required fields" in:
      CreateInvitationV3Request.parse(Some(Json.obj("service" -> "MTD-IT"))) shouldBe Left(InvalidPayload)
      CreateInvitationV3Request.parse(Some(Json.obj("service" -> "MTD-IT", "clientId" -> 123))) shouldBe
        Left(InvalidPayload)

  "CreateInvitationV3Request.resolveService" should:
    "resolve every supported V3 service" in:
      ApiService.values.foreach: service =>
        val request = CreateInvitationV3Request(service.value, "client-id", None, None)

        request.resolveService shouldBe Right(service)

    "return SERVICE_NOT_SUPPORTED for an unknown service" in:
      val request = CreateInvitationV3Request("mtd-it", "client-id", None, None)

      request.resolveService shouldBe Left(UnsupportedService)

  "CreateInvitationV3Request.resolveClientId" should:
    "recognise every supported client identifier format" in:
      val clientIds = Seq(
        "XAPLR1234567890",
        "XACBC1234567890",
        "XACGTP123456789",
        "1234567890",
        "XXTRUST12345678",
        "AB123456A",
        "XAPPT0001234567",
        "101747696"
      )

      clientIds.foreach: clientId =>
        val request = CreateInvitationV3Request("MTD-IT", clientId, None, None)

        request.resolveClientId.map(_.value) shouldBe Right(clientId)

    "return CLIENT_ID_FORMAT_INVALID when no supported format matches" in:
      val request = CreateInvitationV3Request("MTD-IT", "not-a-client-id", None, None)

      request.resolveClientId shouldBe Left(ClientIdInvalidFormat)

  "CreateInvitationV3Request.resolveAgentType" should:
    "allow the agent type to be omitted" in:
      val request = CreateInvitationV3Request("MTD-IT", "AB123456A", None, None)

      request.resolveAgentType shouldBe Right(None)

    "resolve main and supporting agent types" in:
      Seq("main" -> AgentType.Main, "supporting" -> AgentType.Supporting).foreach: (value, expected) =>
        val request = CreateInvitationV3Request("MTD-IT", "AB123456A", None, Some(value))

        request.resolveAgentType shouldBe Right(Some(expected))

    "return AGENT_TYPE_NOT_SUPPORTED for any other supplied value" in:
      Seq("Main", "other").foreach: value =>
        val request = CreateInvitationV3Request("MTD-IT", "AB123456A", None, Some(value))

        request.resolveAgentType shouldBe Left(UnsupportedAgentType)

  "CreateInvitationV3Request.validateKnownFactPresence" should:
    "allow a known fact to be omitted for trusts" in:
      val request = CreateInvitationV3Request("TRUSTS", "1234567890", None, None)

      request.validateKnownFactPresence(ApiService.Trusts) shouldBe Right(None)

    "require a known fact for every other supported service" in:
      ApiService.values.filterNot(_ == ApiService.Trusts).foreach: service =>
        val request = CreateInvitationV3Request(service.value, "client-id", None, None)

        request.validateKnownFactPresence(service) shouldBe Left(InvalidPayload)

    "retain a supplied known fact for every supported service" in:
      ApiService.values.foreach: service =>
        val request = CreateInvitationV3Request(service.value, "client-id", Some("known-fact"), None)

        request.validateKnownFactPresence(service) shouldBe Right(Some("known-fact"))

  "CreateInvitationV3Request.resolveKnownFact" should:
    "allow the known fact to be omitted for separate service validation" in:
      val request = CreateInvitationV3Request("TRUSTS", "1234567890", None, None)

      request.resolveKnownFact shouldBe Right(None)

    "resolve each supported known-fact format" in:
      val knownFacts = Seq(
        "AA1 1AA" -> ApiKnownFactType.PostalCode,
        "GB" -> ApiKnownFactType.CountryCode,
        "client@example.com" -> ApiKnownFactType.Email,
        "2026-09-22" -> ApiKnownFactType.Date
      )

      knownFacts.foreach: (value, expectedType) =>
        val request = CreateInvitationV3Request("MTD-IT", "AB123456A", Some(value), None)
        val result = request.resolveKnownFact.value.value

        result.value shouldBe value
        result.knownFactType shouldBe expectedType

    "return KNOWN_FACT_FORMAT_INVALID when no supported format matches" in:
      val request = CreateInvitationV3Request("MTD-IT", "AB123456A", Some("not-a-known-fact"), None)

      request.resolveKnownFact shouldBe Left(KnownFactFormatInvalid)

    "return KNOWN_FACT_FORMAT_INVALID for an impossible calendar date" in:
      val request = CreateInvitationV3Request("MTD-VAT", "101747696", Some("2026-02-31"), None)

      request.resolveKnownFact shouldBe Left(KnownFactFormatInvalid)
