/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.Helpers
import uk.gov.hmrc.agentauthorisation.controllers.api.{DocumentationController, YamlController}
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import scala.collection.immutable

class PlatformIntegrationSpec extends BaseISpec {

  val documentationController = app.injector.instanceOf[DocumentationController]
  val yamlController = app.injector.instanceOf[YamlController]
  val request = FakeRequest()

  override def beforeEach(): Unit = {
    stubFor(post(urlMatching("/registration")).willReturn(aResponse().withStatus(NO_CONTENT)))
    super.beforeEach()
  }

  "microservice" should {

    "provide definition endpoint and documentation endpoint for each api" in {
      def verifyDocumentationPresent(version: String, endpointName: String): Unit =
        withClue(s"Getting documentation version '$version' of endpoint '$endpointName'") {
          val documentationResult = documentationController.documentation(version, endpointName)(request)
          status(documentationResult) shouldBe OK
        }

      val result = documentationController.definition()(request)
      status(result) shouldBe OK

      val jsonResponse = Helpers.contentAsJson(result)

      val versions: Seq[String] = (jsonResponse \\ "version").to(immutable.Seq) map (_.as[String])
      val endpointNames: Seq[Seq[String]] =
        (jsonResponse \\ "endpoints")
          .to(immutable.Seq)
          .map(_ \\ "endpointName")
          .to(immutable.Seq)
          .map(_.to(immutable.Seq))
          .map(_.asInstanceOf[Seq[String]])

      versions
        .zip(endpointNames)
        .flatMap { case (version, endpoint) =>
          endpoint.map(endpointName => (version, endpointName))
        }
        .foreach { case (version, endpointName) => verifyDocumentationPresent(version, endpointName) }
    }

    "provide yaml documentation" in {
      val result = yamlController.yaml("1.0", "application.yaml")(request)

      status(result) shouldBe OK
      Helpers.contentAsString(result) should startWith("openapi: 3.0.3")
    }
  }
}
