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

import uk.gov.hmrc.agentauthorisation.support.UnitSpec

class ApiKnownFactSpec extends UnitSpec:

  "ApiKnownFact.from" should:
    "recognise every supported known-fact format" in:
      val knownFacts = Seq(
        "AA1 1AA" -> ApiKnownFactType.PostalCode,
        "BFPO 123" -> ApiKnownFactType.PostalCode,
        "GB" -> ApiKnownFactType.CountryCode,
        "client@example.com" -> ApiKnownFactType.Email,
        "2026-09-22" -> ApiKnownFactType.Date
      )

      knownFacts.foreach: (value, expectedType) =>
        val result = ApiKnownFact.from(value).value

        result.value shouldBe value
        result.knownFactType shouldBe expectedType

    "reject values which match no supported format" in:
      Seq("", "not-a-known-fact", "2026/09/22", "2026-02-31", "2025-02-29", "client@example").foreach: value =>
        ApiKnownFact.from(value) shouldBe None

    "accept a valid leap day" in:
      ApiKnownFact.from("2024-02-29").value.knownFactType shouldBe ApiKnownFactType.Date

    "require complete, case-sensitive matches" in:
      ApiKnownFact.from("prefix AA1 1AA") shouldBe None
      ApiKnownFact.from("aa1 1aa") shouldBe None
