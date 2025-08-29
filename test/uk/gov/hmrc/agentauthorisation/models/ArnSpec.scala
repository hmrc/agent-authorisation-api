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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ArnSpec extends AnyFlatSpec with Matchers {

  it should "be true for a valid ARN" in {
    Arn.isValid("TARN0000001") shouldBe true
  }

  it should "be false when characters 2-4 are not \"ARN\"" in {
    Arn.isValid("TABC0000001") shouldBe false
  }

  it should "be false with lowercase \"arn\"" in {
    Arn.isValid("Tarn0000001") shouldBe false
  }

  it should "be false when empty" in {
    Arn.isValid("") shouldBe false
  }

  it should "be false when non capital first character" in {
    Arn.isValid("tARN0000001") shouldBe false
  }

  it should "be false when too short" in {
    Arn.isValid("TARN00001") shouldBe false
  }

  it should "be false when too long" in {
    Arn.isValid("TARN0000000001") shouldBe false
  }

  it should "be false when the missing the first character doesn't match" in {
    Arn.isValid("ARN0000001") shouldBe false
  }

  it should "be false when the checksum doesn't pass" in {
    Arn.isValid("AARN0000001") shouldBe false
  }
}
