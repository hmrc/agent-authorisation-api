/*
 * Copyright 2021 HM Revenue & Customs
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

package action

import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import play.api.mvc.{Call, RequestHeader, Result}
import play.api.test.FakeRequest
import support.BaseSpec
import uk.gov.hmrc.agentauthorisation.actions.AcceptHeaderFilter
import play.api.mvc.Results._
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class AcceptHeaderFilterSpec extends BaseSpec with MockFactory {

  val servicesConfig = mock[ServicesConfig]
  (servicesConfig
    .baseUrl(_: String))
    .expects(*)
    .atLeastOnce()
    .returning("blah-url")
  (servicesConfig
    .getConfString(_: String, _: String))
    .expects(*, *)
    .atLeastOnce()
    .returning("blah")
  (servicesConfig
    .getInt(_: String))
    .expects(*)
    .atLeastOnce()
    .returning(30)
  (servicesConfig
    .getString(_: String))
    .expects(*)
    .atLeastOnce()
    .returning("some config string")

  val config = Configuration.apply("api.supported-versions" -> List(1.0))

  val appConfig = new AppConfig(servicesConfig, config)

  case class TestAcceptHeaderFilter(supportedVersion: Seq[String]) extends AcceptHeaderFilter(appConfig) {
    def response(f: RequestHeader => Future[Result])(rh: RequestHeader) = await(super.apply(f)(rh))
  }

  object TestAcceptHeaderFilter {

    val testHeaderVersion: String => Seq[(String, String)] =
      (testVersion: String) => Seq("Accept" -> s"application/vnd.hmrc.$testVersion+json")

    def fakeHeaders(headers: Seq[(String, String)]) = testRequest(FakeRequest().withHeaders(headers: _*))

    def fakeHeaders(call: Call, headers: Seq[(String, String)]) =
      testRequest(FakeRequest(call).withHeaders(headers: _*))

    def toResult(result: Result) = (_: RequestHeader) => Future.successful(result)
  }

  import TestAcceptHeaderFilter._

  "AcceptHeaderFilter" should {
    "return None" when {
      "no errors found in request" in {
        val supportedVersions: Seq[String] = Seq("1.0")
        val fakeTestHeader = fakeHeaders(testHeaderVersion("1.0"))
        TestAcceptHeaderFilter(supportedVersions).response(toResult(Ok("")))(fakeTestHeader) shouldBe Ok("")
      }

      "uri is /ping/ping with no headers" in {
        val call = Call("GET", "/ping/ping")
        val fakeTestHeader = fakeHeaders(call, Seq.empty)
        TestAcceptHeaderFilter(Seq.empty).response(toResult(Ok("")))(fakeTestHeader) shouldBe Ok("")
      }

      "uri is /api/conf/:version/*file with no headers" in {
        val call = Call("GET", "/api/conf/1.0/someFile")
        val fakeTestHeader = fakeHeaders(call, Seq.empty)
        TestAcceptHeaderFilter(Seq.empty).response(toResult(Ok("")))(fakeTestHeader) shouldBe Ok("")
      }

      "uri is /api/definition with no headers" in {
        val call = Call("GET", "/api/definition")
        val fakeTestHeader = fakeHeaders(call, Seq.empty)
        TestAcceptHeaderFilter(Seq.empty).response(toResult(Ok("")))(fakeTestHeader) shouldBe Ok("")
      }
      "uri is /api/documentation/:version/:endpointName with no headers" in {
        val call = Call("GET", "/api/documentation/1.0/someEndpointName")
        val fakeTestHeader = fakeHeaders(call, Seq.empty)
        TestAcceptHeaderFilter(Seq.empty).response(toResult(Ok("")))(fakeTestHeader) shouldBe Ok("")
      }
    }

    "return Some" when {
      "request had no Accept Header" in {
        val supportedVersions: Seq[String] = Seq("1.0")
        val fakeTestHeader = fakeHeaders(Seq.empty)
        val result = TestAcceptHeaderFilter(supportedVersions).response(toResult(Ok))(fakeTestHeader)
        bodyOf(result) shouldBe """{"code":"ACCEPT_HEADER_INVALID","message":"Missing 'Accept' header."}"""
      }

      "request had an invalid Accept Header" in {
        val supportedVersions: Seq[String] = Seq("1.0")
        val fakeTestHeader = fakeHeaders(Seq("Accept" -> s"InvalidHeader"))
        val result = TestAcceptHeaderFilter(supportedVersions).response(toResult(Ok))(fakeTestHeader)
        bodyOf(result) shouldBe """{"code":"ACCEPT_HEADER_INVALID","message":"Invalid 'Accept' header."}"""
      }

      "request used an unsupported version" in {
        val supportedVersions: Seq[String] = Seq("1.0")
        val fakeTestHeader = fakeHeaders(testHeaderVersion("0.0"))
        val result = TestAcceptHeaderFilter(supportedVersions).response(toResult(Ok))(fakeTestHeader)
        bodyOf(result) shouldBe """{"code":"BAD_REQUEST","message":"Missing or unsupported version number."}"""
      }

      "request used an unsupported content-type" in {
        val supportedVersions: Seq[String] = Seq("1.0")
        val fakeTestHeader = fakeHeaders(Seq("Accept" -> s"application/vnd.hmrc.1.0+xml"))
        val result = TestAcceptHeaderFilter(supportedVersions).response(toResult(Ok))(fakeTestHeader)
        bodyOf(result) shouldBe """{"code":"BAD_REQUEST","message":"Missing or unsupported content-type."}"""
      }
    }
  }
}
