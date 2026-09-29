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

package uk.gov.hmrc.agentauthorisation.models

import org.scalatest.EitherValues
import uk.gov.hmrc.agentauthorisation.support.UnitSpec

class ApiServiceSpec extends UnitSpec with EitherValues:

  private val validServiceClients = Seq(
    "PILLAR2" -> "XAPLR1234567890",
    "CBC"     -> "XACBC1234567890",
    "CGT-PD"  -> "XACGTP123456789",
    "TRUSTS"  -> "1234567890",
    "TRUSTS"  -> "XXTRUST12345678",
    "IRV"     -> "AB123456A",
    "PPT"     -> "XAPPT0001234567",
    "MTD-VAT" -> "101747696",
    "MTD-IT"  -> "AB123456A"
  )

  "ApiServiceClient.validate" should:
    "accept every supported service and client identifier format" in:
      validServiceClients.foreach: (service, clientId) =>
        withClue(service):
          val result = ApiServiceClient.validate(service, clientId).value
          result.service.value shouldBe service
          result.clientId.value shouldBe clientId

    "reject an unsupported service before validating its client identifier" in:
      ApiServiceClient.validate("SA", "invalid") shouldBe Left(UnsupportedService)

    "reject a client identifier which matches no supported format" in:
      ApiServiceClient.validate("MTD-IT", "invalid") shouldBe Left(ClientIdInvalidFormat)

    "reject a valid client identifier which is incompatible with the service" in:
      validServiceClients.foreach: (service, clientId) =>
        val otherService = if service == "MTD-VAT" then "MTD-IT" else "MTD-VAT"
        withClue(s"$service identifier against $otherService"):
          ApiServiceClient.validate(otherService, clientId) shouldBe Left(ClientIdIncompatibleWithService)

    "reject partial matches to a supported format" in:
      ApiServiceClient.validate("TRUSTS", "12345678901") shouldBe Left(ClientIdInvalidFormat)
      ApiServiceClient.validate("PPT", "XAPPT0001234567X") shouldBe Left(ClientIdInvalidFormat)

    "treat service names as exact public contract values" in:
      ApiServiceClient.validate("mtd-it", "AB123456A") shouldBe Left(UnsupportedService)
