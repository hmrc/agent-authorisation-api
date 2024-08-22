/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.controllers.api

import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, Resource}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class ApiPlatformISpec extends BaseISpec {

  implicit val ws: WSClient = app.injector.instanceOf[WSClient]

  implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq("Accept" -> s"application/vnd.hmrc.1.0+json"))

  "/public/api/definition" should {
    "return the definition JSON" in {
      val response: HttpResponse = new Resource(s"/api/definition", port).get()
      response.status shouldBe 200

      val definition = response.json

      (definition \ "api" \ "name").as[String] shouldBe "Agent Authorisation"

      val accessConfig = definition \ "api" \ "versions" \\ "access"
      (accessConfig.head \ "type").as[String] shouldBe "PRIVATE"
    }
  }

  "provide YAML documentation exists for all API versions" in new ApiTestSupport {

    lazy override val runningPort: Int = port

    forAllApiVersions(yamlByVersion) { case (version, yaml) =>
      info(s"Checking API YAML documentation for version[$version] of the API")

      withClue("YAML does not contain a valid YAML 1.0 version header") {
        yaml should include("""version: '1.0'""")
      }

      withClue("YAML does not contain the title 'Agent Authorisation'") {
        yaml should include("title: Agent Authorisation")

      }

      withClue(s"YAML does not contain a matching version declaration of [$version]") {
        yaml should include(s"""version: '$version'""")
      }
    }
  }
}
