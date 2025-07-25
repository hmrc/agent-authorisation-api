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

import uk.gov.hmrc.agentauthorisation.models.Service.ItsaMain
import uk.gov.hmrc.agentauthorisation.support.BaseSpec

class ClientAccessDataSpec extends BaseSpec {
  val nino = "AB123456A"
  val validPostcode = "DH14EJ"

  "ClientAccessData" should {

    "read from payload when the create invitation payload model is valid" in {
      val payload = CreateInvitationPayload(
        service = List("MTD-IT"),
        clientType = "personal",
        clientIdType = "ni",
        clientId = nino,
        knownFact = validPostcode,
        agentType = Some("main")
      )

      ClientAccessData.unapply(payload) shouldBe Some(
        ClientAccessData(
          service = ItsaMain,
          suppliedClientId = nino,
          knownFact = validPostcode,
          clientType = Some("personal")
        )
      )
    }

    "read from payload when the check relationship payload model is valid" in {
      val payload = CheckRelationshipPayload(
        service = List("MTD-IT"),
        clientIdType = "ni",
        clientId = nino,
        knownFact = validPostcode,
        agentType = Some("main")
      )

      ClientAccessData.unapply(payload) shouldBe Some(
        ClientAccessData(
          service = ItsaMain,
          suppliedClientId = nino,
          knownFact = validPostcode,
          clientType = None
        )
      )
    }

    "return None when payload is not valid nor caught by other errors" in {
      val invalidPayload = CreateInvitationPayload(
        service = List("MTD-IT"),
        clientType = "business",
        clientIdType = "ni",
        clientId = nino,
        knownFact = validPostcode,
        agentType = Some("xxx")
      )

      ClientAccessData.unapply(invalidPayload) shouldBe None
    }

  }
}
