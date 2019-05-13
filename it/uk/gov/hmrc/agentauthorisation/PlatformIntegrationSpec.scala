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


import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, TestData}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.{Application, Mode}
import uk.gov.hmrc.agentauthorisation.controllers.api.{DocumentationController, RamlController}
import uk.gov.hmrc.play.test.UnitSpec

class PlatformIntegrationSpec extends UnitSpec with GuiceOneAppPerTest with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val stubHost = "localhost"
  val stubPort = sys.env.getOrElse("WIREMOCK_SERVICE_LOCATOR_PORT", "11112").toInt
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  override def newAppForTest(testData: TestData): Application = GuiceApplicationBuilder()
    .configure("run.mode" -> "Stub")
    .configure(Map(
      "appName" -> "application-name",
      "appUrl" -> "http://example.com",
      "auditing.enabled" -> false))
    .in(Mode.Test).build()

  override def beforeEach() {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
    stubFor(post(urlMatching("/registration")).willReturn(aResponse().withStatus(NO_CONTENT)))
  }

  trait Setup {
    implicit def mat: akka.stream.Materializer = app.injector.instanceOf[akka.stream.Materializer]
    val documentationController = app.injector.instanceOf[DocumentationController]
    val ramlController = app.injector.instanceOf[RamlController]
    val request = FakeRequest()
  }

  "microservice" should {

    "provide definition endpoint and documentation endpoint for each api" in new Setup {
      def normalizeEndpointName(endpointName: String): String = endpointName.replaceAll(" ", "-")

      def verifyDocumentationPresent(version: String, endpointName: String) {
        withClue(s"Getting documentation version '$version' of endpoint '$endpointName'") {
          val documentationResult = documentationController.documentation(version, endpointName)(request)
          status(documentationResult) shouldBe OK
        }
      }

      val result = documentationController.definition()(request)
      status(result) shouldBe OK

      val jsonResponse = jsonBodyOf(result).futureValue

      val versions: Seq[String] = (jsonResponse \\ "version") map (_.as[String])
      val endpointNames: Seq[Seq[String]] = (jsonResponse \\ "endpoints").map(_ \\ "endpointName").map(_.map(_.as[String]))

      versions.zip(endpointNames).flatMap {
        case (version, endpoint) => endpoint.map(endpointName => (version, endpointName))
      }.foreach { case (version, endpointName) => verifyDocumentationPresent(version, endpointName) }
    }

    "provide raml documentation" in new Setup {
      val result = ramlController.raml("1.0", "application.raml")(request)

      status(result) shouldBe OK
      bodyOf(result).futureValue should startWith("#%RAML 1.0")
    }
  }

  override protected def afterEach(): Unit = {
    wireMockServer.stop()
    wireMockServer.resetMappings()
  }
}
