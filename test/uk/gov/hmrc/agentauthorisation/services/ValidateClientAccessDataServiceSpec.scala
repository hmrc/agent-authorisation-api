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

package uk.gov.hmrc.agentauthorisation.services

import play.api.libs.json.Json
import uk.gov.hmrc.agentauthorisation.models.Service.ItsaMain
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.BaseSpec

class ValidateClientAccessDataServiceSpec extends BaseSpec {

  val testService = new ValidateClientAccessDataService()

  "validateCreateInvitationPayload" should {
    "return None when JsValue is missing" in {
      testService.validateCreateInvitationPayload(None) shouldBe Left(InvalidPayload)
    }
    "return UnsupportedClientType when create invitation payload includes unsupported client type" in {
      testService.validateCreateInvitationPayload(
        Some(
          Json.obj(
            "service"      -> Json.arr("MTD-IT"),
            "clientId"     -> "NL019207B",
            "clientIdType" -> "ni",
            "clientType"   -> "business",
            "knownFact"    -> "BN111XG",
            "agentType"    -> "main"
          )
        )
      ) shouldBe Left(UnsupportedClientType)
    }
    "return PostcodeFormatInvalid when payload includes known fact that fails postcode regex" in {
      testService.validateCreateInvitationPayload(
        Some(
          Json.obj(
            "service"      -> Json.arr("MTD-IT"),
            "clientId"     -> "NL019207B",
            "clientIdType" -> "ni",
            "clientType"   -> "personal",
            "knownFact"    -> "23BC",
            "agentType"    -> "main"
          )
        )
      ) shouldBe Left(PostcodeFormatInvalid)
    }

  }

  "validateCheckRelationshipPayload" should {
    "return None when JsValue is missing" in {
      testService.validateCheckRelationshipPayload(None) shouldBe Left(InvalidPayload)
    }
    "return success check relationship payload is valid" in {
      testService.validateCheckRelationshipPayload(
        Some(
          Json.obj(
            "service"      -> Json.arr("MTD-IT"),
            "clientId"     -> "NL019207B",
            "clientIdType" -> "ni",
            "knownFact"    -> "BN111XG",
            "agentType"    -> "main"
          )
        )
      ) shouldBe Right(
        ClientAccessData(
          service = ItsaMain,
          suppliedClientId = "NL019207B",
          knownFact = "BN111XG",
          clientType = None
        )
      )
    }
    "return PostcodeFormatInvalid when payload includes known fact that fails postcode regex" in {
      testService.validateCheckRelationshipPayload(
        Some(
          Json.obj(
            "service"      -> Json.arr("MTD-IT"),
            "clientId"     -> "NL019207B",
            "clientIdType" -> "ni",
            "knownFact"    -> "23BC"
          )
        )
      ) shouldBe Left(PostcodeFormatInvalid)
    }

  }

  "validateDeleteRelationshipPayload" should {
    "return InvalidPayload when JsValue is missing" in {
      testService.validateDeleteRelationshipPayload(None) shouldBe Left(InvalidPayload)
    }

    "return UnsupportedService when service is not supported" in {
      testService.validateDeleteRelationshipPayload(
        Some(
          Json.obj(
            "service"      -> Json.arr("MTD-EOPS"),
            "clientId"     -> "NL019207B",
            "clientIdType" -> "ni",
            "clientType"   -> "personal"
          )
        )
      ) shouldBe Left(UnsupportedService)
    }

    "return UnsupportedClientType when client type is not supported for service" in {
      testService.validateDeleteRelationshipPayload(
        Some(
          Json.obj(
            "service"      -> Json.arr("MTD-IT"),
            "clientId"     -> "NL019207B",
            "clientIdType" -> "ni",
            "clientType"   -> "business"
          )
        )
      ) shouldBe Left(UnsupportedClientType)
    }

    "return ClientIdDoesNotMatchService when clientIdType does not match service" in {
      testService.validateDeleteRelationshipPayload(
        Some(
          Json.obj(
            "service"      -> Json.arr("MTD-IT"),
            "clientId"     -> "NL019207B",
            "clientIdType" -> "vrn",
            "clientType"   -> "personal"
          )
        )
      ) shouldBe Left(ClientIdDoesNotMatchService)
    }

    "return ClientIdInvalidFormat when clientId does not match pattern for service" in {
      testService.validateDeleteRelationshipPayload(
        Some(
          Json.obj(
            "service"      -> Json.arr("MTD-VAT"),
            "clientId"     -> "NOT_A_VRN",
            "clientIdType" -> "vrn",
            "clientType"   -> "business"
          )
        )
      ) shouldBe Left(ClientIdInvalidFormat)
    }

    "return UnsupportedAgentType when agentType is invalid for ITSA" in {
      testService.validateDeleteRelationshipPayload(
        Some(
          Json.obj(
            "service"      -> Json.arr("MTD-IT"),
            "clientId"     -> "NL019207B",
            "clientIdType" -> "ni",
            "clientType"   -> "personal",
            "agentType"    -> "invalid"
          )
        )
      ) shouldBe Left(UnsupportedAgentType)
    }

    "return payload when delete relationship payload is valid" in {
      val result = testService.validateDeleteRelationshipPayload(
        Some(
          Json.obj(
            "service"      -> Json.arr("MTD-VAT"),
            "clientId"     -> "101747696",
            "clientIdType" -> "vrn",
            "clientType"   -> "business"
          )
        )
      )

      result shouldBe Right(
        DeleteRelationshipPayload(
          service = List("MTD-VAT"),
          clientType = "business",
          clientIdType = "vrn",
          clientId = "101747696",
          agentType = None
        )
      )
    }
  }
}
