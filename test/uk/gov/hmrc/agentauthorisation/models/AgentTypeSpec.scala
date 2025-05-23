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

import uk.gov.hmrc.agentauthorisation.support.BaseSpec

class AgentTypeSpec extends BaseSpec {

  "AgentType" should {
    "accept a valid string value" in {
      AgentType("main") shouldBe AgentType.Main
      AgentType("supporting") shouldBe AgentType.Supporting
    }
    "have the correct agentTypeName for the type" in {
      AgentType.Main.agentTypeName shouldBe "main"
      AgentType.Supporting.agentTypeName shouldBe "supporting"
    }
  }
}
