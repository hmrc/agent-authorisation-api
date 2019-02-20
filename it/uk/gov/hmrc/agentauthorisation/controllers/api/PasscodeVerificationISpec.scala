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

import java.io.File

import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Results.Ok
import play.api.test.{FakeApplication, FakeRequest}
import play.api.{Configuration, Environment, Mode, Play}
import uk.gov.hmrc.agentauthorisation.support.AkkaMaterializerSpec
import uk.gov.hmrc.auth.otac.{Authorised, OtacAuthConnector, Unauthorised}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class PasscodeVerificationISpec extends UnitSpec with MockitoSugar with AkkaMaterializerSpec with BeforeAndAfterAll {

  val hc: HeaderCarrier = HeaderCarrier()
  val ec: ExecutionContext = concurrent.ExecutionContext.Implicits.global
  val environment = Environment.apply(new File("."), this.getClass.getClassLoader, Mode.Prod)
  val body = (b: Boolean) => Future.successful(Ok(b.toString))
  lazy val app = FakeApplication()

  protected override def beforeAll() =
    Play.start(app)

  protected override def afterAll() =
    Play.stop(app)

  "PasscodeVerification" when {

    "passcodeAuthentication disabled" should {
      "execute function with 'true'" in {
        val otacAuthConnector = mock[OtacAuthConnector]
        val withMaybePasscode = new FrontendPasscodeVerification(
          Configuration.from(
            Map("passcodeAuthentication.enabled" -> false, "passcodeAuthentication.regime" -> "fooRegime")),
          environment,
          otacAuthConnector)
        val request = FakeRequest("GET", "/")
        val result = await(withMaybePasscode(body)(request, hc, ec))
        status(result) shouldBe 200
        bodyOf(result) shouldBe "true"
      }
    }

    "passcodeAuthentication enabled" should {

      val configuration = Configuration.from(
        Map("passcodeAuthentication.enabled" -> true, "passcodeAuthentication.regime" -> "fooRegime"))

      "execute function with 'false' if not otac param nor session key present" in {
        val otacAuthConnector = mock[OtacAuthConnector]
        val withMaybePasscode = new FrontendPasscodeVerification(configuration, environment, otacAuthConnector)
        val request = FakeRequest("GET", "/")
        val result = await(withMaybePasscode(body)(request, hc, ec))
        status(result) shouldBe 200
        bodyOf(result) shouldBe "false"
      }
      "redirect to otac verification if otac param present" in {
        val otacAuthConnector = mock[OtacAuthConnector]
        val withMaybePasscode = new FrontendPasscodeVerification(configuration, environment, otacAuthConnector)
        val request = FakeRequest("GET", "/foo/bar/?p=otac123")
        val result = await(withMaybePasscode(body)(request, hc, ec))
        status(result) shouldBe 303
        result.header.headers.get("Location") shouldBe Some("/verification/otac/login?p=otac123")
      }
      "call auth service if otac session key present and execute body with true if authorised" in {
        val otacAuthConnector = mock[OtacAuthConnector]
        val withMaybePasscode = new FrontendPasscodeVerification(configuration, environment, otacAuthConnector)
        when(otacAuthConnector.authorise("fooRegime", hc, Some("fooOTACToken")))
          .thenReturn(Authorised)
        val request = FakeRequest("GET", "/foo/bar/").withSession((SessionKeys.otacToken, "fooOTACToken"))
        val result = await(withMaybePasscode(body)(request, hc, ec))
        status(result) shouldBe 200
        bodyOf(result) shouldBe "true"
      }
      "call auth service if otac session key present and execute body with false if unauthorised" in {
        val otacAuthConnector = mock[OtacAuthConnector]
        val withMaybePasscode = new FrontendPasscodeVerification(configuration, environment, otacAuthConnector)
        when(otacAuthConnector.authorise("fooRegime", hc, Some("fooOTACToken")))
          .thenReturn(Unauthorised)
        val request = FakeRequest("GET", "/foo/bar/").withSession((SessionKeys.otacToken, "fooOTACToken"))
        val result = await(withMaybePasscode(body)(request, hc, ec))
        status(result) shouldBe 200
        bodyOf(result) shouldBe "false"
      }
    }
  }
}
