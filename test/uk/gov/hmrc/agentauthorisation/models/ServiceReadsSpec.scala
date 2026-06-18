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

import play.api.libs.json.Json
import uk.gov.hmrc.agentauthorisation.support.BaseSpec

class ServiceReadsSpec extends BaseSpec {

  private val createInvitationJson = Json.obj(
    "service"      -> "MTD-IT",
    "clientType"   -> "personal",
    "clientIdType" -> "ni",
    "clientId"     -> "AA123456A",
    "knownFact"    -> "AA11 1AA",
    "agentType"    -> "main"
  )

  "CreateInvitationPayload" should {
    "read service when it is a string" in {
      createInvitationJson.validate[CreateInvitationPayload].isSuccess shouldBe true
    }

    "reject service arrays with more than one item" in {
      val invalidJson = createInvitationJson + ("service" -> Json.arr("MTD-IT", "MTD-VAT"))

      invalidJson.validate[CreateInvitationPayload].isError shouldBe true
    }
  }

  "CheckRelationshipPayload" should {
    "read service when it is a string" in {
      Json.obj(
        "service"      -> "MTD-VAT",
        "clientIdType" -> "vrn",
        "clientId"     -> "101747696",
        "knownFact"    -> "2025-01-31"
      ).validate[CheckRelationshipPayload].isSuccess shouldBe true
    }
  }

  "DeleteRelationshipPayload" should {
    "read service when it is a string" in {
      Json.obj(
        "service"      -> "MTD-IT",
        "clientType"   -> "personal",
        "clientIdType" -> "ni",
        "clientId"     -> "AA123456A"
      ).validate[DeleteRelationshipPayload].isSuccess shouldBe true
    }
  }
}
