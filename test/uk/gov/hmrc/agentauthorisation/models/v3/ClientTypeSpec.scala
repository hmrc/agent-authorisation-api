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

class ClientTypeSpec extends UnitSpec:

  "ClientType JSON" should:
    "preserve the wire values" in:
      ClientType.values.foreach: clientType =>
        Json.toJson(clientType) shouldBe JsString(clientType.value)
        JsString(clientType.value).validate[ClientType] shouldBe JsSuccess(clientType)

    "reject an unsupported wire value" in:
      JsString("organisation").validate[ClientType] shouldBe a[JsError]
