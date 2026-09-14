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

package uk.gov.hmrc.agentauthorisation.config

import play.api.Configuration
import uk.gov.hmrc.agentauthorisation.support.UnitSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfigSpec extends UnitSpec {

  private def servicesConfig(configuration: Configuration) =
    ServicesConfig(configuration)

  "AppConfig" should {
    "read service URLs and API settings from configuration" in {
      val configuration = Configuration(
        "api.supported-versions"                                                 -> List("1.0", "2.0"),
        "api.access"                                                        -> "PRIVATE",
        "microservice.services.agent-client-relationships.host"                  -> "localhost",
        "microservice.services.agent-client-relationships.port"                  -> 9434,
        "microservice.services.agent-client-relationships-frontend.external-url" -> "http://localhost:9435"
      )

      val appConfig = AppConfig(servicesConfig(configuration), configuration)

      appConfig.acrBaseUrl shouldBe "http://localhost:9434"
      appConfig.acrfExternalUrl shouldBe "http://localhost:9435"
      appConfig.apiType shouldBe "PRIVATE"
      appConfig.apiSupportedVersions.toArray.toSeq shouldBe Seq("1.0", "2.0")
    }

    "fail clearly when agent-client-relationships-frontend.external-url is missing" in {
      val configuration = Configuration(
        "api.supported-versions"                                -> List("1.0"),
        "api.access"                                       -> "PRIVATE",
        "microservice.services.agent-client-relationships.host" -> "localhost",
        "microservice.services.agent-client-relationships.port" -> 9434
      )

      val exception = intercept[RuntimeException]:
        AppConfig(servicesConfig(configuration), configuration)

      exception.getMessage.shouldBe("config 'agent-client-relationships-frontend.external-url' not found")
    }
  }
}
