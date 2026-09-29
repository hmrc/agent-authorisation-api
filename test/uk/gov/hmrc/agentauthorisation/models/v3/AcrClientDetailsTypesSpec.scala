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
import uk.gov.hmrc.agentauthorisation.support.UnitSpec

class AcrClientDetailsTypesSpec extends UnitSpec:

  "AcrClientDetails.Status JSON" should:
    "preserve every ACR wire value" in:
      AcrClientDetails.Status.values.foreach: status =>
        Json.toJson(status) shouldBe JsString(status.value)
        JsString(status.value).validate[AcrClientDetails.Status] shouldBe JsSuccess(status)

    "reject an unsupported wire value" in:
      JsString("Active").validate[AcrClientDetails.Status] shouldBe a[JsError]

  "AcrClientDetails.KnownFactType JSON" should:
    "preserve every ACR wire value" in:
      AcrClientDetails.KnownFactType.values.foreach: knownFactType =>
        Json.toJson(knownFactType) shouldBe JsString(knownFactType.value)
        JsString(knownFactType.value).validate[AcrClientDetails.KnownFactType] shouldBe JsSuccess(knownFactType)

    "reject an unsupported wire value" in:
      JsString("Utr").validate[AcrClientDetails.KnownFactType] shouldBe a[JsError]

    "map every API known fact type explicitly" in:
      ApiKnownFactType.values.map(AcrClientDetails.KnownFactType.fromApi) should contain theSameElementsAs Seq(
        AcrClientDetails.KnownFactType.PostalCode,
        AcrClientDetails.KnownFactType.CountryCode,
        AcrClientDetails.KnownFactType.Email,
        AcrClientDetails.KnownFactType.Date
      )
