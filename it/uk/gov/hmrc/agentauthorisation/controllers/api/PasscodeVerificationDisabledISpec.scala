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
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class PasscodeVerificationDisabledISpec extends BaseISpec {

  val hc: HeaderCarrier = HeaderCarrier()
  val ec: ExecutionContext = concurrent.ExecutionContext.Implicits.global
  val body = (b: Boolean) => Future.successful(Ok(b.toString))

  val frontendPasscodeVerification = app.injector.instanceOf[FrontendPasscodeVerification]

  override def additionalConfiguration: Map[String, Any] =
    Map("passcodeAuthentication.enabled" -> false, "passcodeAuthentication.regime" -> "fooRegime")

  "PasscodeVerification" when {
    "passcodeAuthentication disabled" should {
      "execute function with 'true'" in {
        val request = FakeRequest("GET", "/")
        val result = await(frontendPasscodeVerification(body)(request, hc, ec))
        status(result) shouldBe 200
        bodyOf(result) shouldBe "true"
      }
    }
  }
}
