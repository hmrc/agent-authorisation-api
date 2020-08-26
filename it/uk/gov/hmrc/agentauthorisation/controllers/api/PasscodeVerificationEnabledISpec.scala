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

import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.{ExecutionContext, Future}

class PasscodeVerificationEnabledISpec extends BaseISpec {

  val hc: HeaderCarrier = HeaderCarrier()
  val ec: ExecutionContext = concurrent.ExecutionContext.Implicits.global
  val body = (b: Boolean) => Future.successful(Ok(b.toString))

  val frontendPasscodeVerification = app.injector.instanceOf[FrontendPasscodeVerification]

  override def additionalConfiguration: Map[String, Any] =
    Map("passcodeAuthentication.enabled" -> true, "passcodeAuthentication.regime" -> "fooRegime")

  "PasscodeVerification" when {
    "passcodeAuthentication enabled" should {

      "execute function with 'false' if not otac param nor session key present" in {
        val request = FakeRequest("GET", "/")
        val result = await(frontendPasscodeVerification(body)(request, hc, ec))
        status(result) shouldBe 200
        bodyOf(result) shouldBe "false"
      }

      "redirect to otac verification if otac param present" in {
        val request = FakeRequest("GET", "/foo/bar/?p=otac123")
        val result = await(frontendPasscodeVerification(body)(request, hc, ec))
        status(result) shouldBe 303
        result.header.headers.get("Location") shouldBe Some("/verification/otac/login?p=otac123")
      }

      "call auth service if otac session key present and execute body with true if authorised" in {
        givenOtacAuthorised()
        val request = FakeRequest("GET", "/foo/bar/").withSession((SessionKeys.otacToken, "fooOTACToken"))
        val result = await(frontendPasscodeVerification(body)(request, hc, ec))
        status(result) shouldBe 200
        bodyOf(result) shouldBe "true"
      }
      "call auth service if otac session key present and execute body with false if unauthorised" in {
        givenOtacUnAuthorised()
        val request = FakeRequest("GET", "/foo/bar/").withSession((SessionKeys.otacToken, "fooOTACToken"))
        val result = await(frontendPasscodeVerification(body)(request, hc, ec))
        status(result) shouldBe 200
        bodyOf(result) shouldBe "false"
      }
    }
  }
}
