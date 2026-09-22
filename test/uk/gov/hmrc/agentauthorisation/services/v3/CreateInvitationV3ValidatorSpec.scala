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

package uk.gov.hmrc.agentauthorisation.services.v3

import org.scalatest.EitherValues
import play.api.libs.json.Json
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.models.v3.{ApiKnownFactType, ApiServiceConfiguration}
import uk.gov.hmrc.agentauthorisation.support.UnitSpec

class CreateInvitationV3ValidatorSpec extends UnitSpec with EitherValues:

  private val validRequests = Seq(
    ("MTD-IT", "AB123456A", Some("AA1 1AA"), "HMRC-MTD-IT", "ni", Some(ApiKnownFactType.PostalCode)),
    ("MTD-VAT", "101747696", Some("2020-01-01"), "HMRC-MTD-VAT", "vrn", Some(ApiKnownFactType.Date)),
    ("TRUSTS", "1234567890", None, "HMRC-TERS-ORG", "utr", None),
    ("TRUSTS", "XXTRUST12345678", None, "HMRC-TERSNT-ORG", "urn", None),
    ("IRV", "AB123456A", Some("1990-01-01"), "PERSONAL-INCOME-RECORD", "ni", Some(ApiKnownFactType.Date)),
    ("CGT-PD", "XACGTP123456789", Some("GB"), "HMRC-CGT-PD", "CGTPDRef", Some(ApiKnownFactType.CountryCode)),
    ("PPT", "XAPPT0001234567", Some("2020-01-01"), "HMRC-PPT-ORG", "EtmpRegistrationNumber", Some(ApiKnownFactType.Date)),
    ("CBC", "XACBC1234567890", Some("client@example.com"), "HMRC-CBC-ORG", "cbcId", Some(ApiKnownFactType.Email)),
    ("PILLAR2", "XAPLR1234567890", Some("2020-01-01"), "HMRC-PILLAR2-ORG", "PLRID", Some(ApiKnownFactType.Date))
  )

  "validate" should:
    "validate the complete service contract and select the ACR lookup configuration" in:
      validRequests.foreach: (service, clientId, knownFact, detailsService, idType, factType) =>
        val payload = Json.obj(
          "service" -> service,
          "clientId" -> clientId,
          "knownFact" -> knownFact
        )

        val command = CreateInvitationV3Validator.validate(Some(payload)).value
        val configuration = ApiServiceConfiguration.forCommand(command.service, command.clientId)

        configuration.detailsService shouldBe detailsService
        configuration.suppliedClientIdType shouldBe idType
        command.knownFact.map(_.knownFactType) shouldBe factType

    "distinguish an invalid identifier from one belonging to another service" in:
      CreateInvitationV3Validator.validate(
        Some(Json.obj("service" -> "MTD-IT", "clientId" -> "invalid", "knownFact" -> "AA1 1AA"))
      ) shouldBe Left(ClientIdInvalidFormat)

      CreateInvitationV3Validator.validate(
        Some(Json.obj("service" -> "MTD-IT", "clientId" -> "101747696", "knownFact" -> "AA1 1AA"))
      ) shouldBe Left(ClientIdNotCompatibleWithService)

    "distinguish an invalid known fact from one belonging to another service" in:
      CreateInvitationV3Validator.validate(
        Some(Json.obj("service" -> "MTD-IT", "clientId" -> "AB123456A", "knownFact" -> "invalid"))
      ) shouldBe Left(KnownFactFormatInvalid)

      CreateInvitationV3Validator.validate(
        Some(Json.obj("service" -> "MTD-IT", "clientId" -> "AB123456A", "knownFact" -> "2020-01-01"))
      ) shouldBe Left(KnownFactIncompatibleWithService)

    "require a known fact except for TRUSTS" in:
      CreateInvitationV3Validator.validate(
        Some(Json.obj("service" -> "MTD-VAT", "clientId" -> "101747696"))
      ) shouldBe Left(InvalidPayload)

    "validate an optional agent type" in:
      val base = Json.obj("service" -> "MTD-IT", "clientId" -> "AB123456A", "knownFact" -> "AA1 1AA")

      CreateInvitationV3Validator.validate(Some(base + ("agentType" -> Json.toJson("other")))) shouldBe
        Left(UnsupportedAgentType)
      CreateInvitationV3Validator.validate(Some(base + ("agentType" -> Json.toJson("supporting")))).value.agentType shouldBe
        Some(AgentType.Supporting)
