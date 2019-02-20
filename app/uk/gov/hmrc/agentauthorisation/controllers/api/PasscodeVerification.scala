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

package uk.gov.hmrc.agentauthorisation.controllers.api

import javax.inject.Inject

import play.api.mvc.Results._
import play.api.mvc.{Request, Result}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.auth.otac.{Authorised, OtacAuthConnector}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.{ExecutionContext, Future}

class PasscodeVerificationException(msg: String) extends RuntimeException(msg)

trait PasscodeVerification {
  def apply[A](body: Boolean => Future[Result])(
    implicit
    request: Request[A],
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext): Future[Result]
}

class FrontendPasscodeVerification @Inject()(
  configuration: Configuration,
  environment: Environment,
  otacAuthConnector: OtacAuthConnector)
    extends PasscodeVerification {

  val tokenParam = "p"
  val passcodeEnabledKey = "passcodeAuthentication.enabled"
  val passcodeRegimeKey = "passcodeAuthentication.regime"

  lazy val passcodeEnabled: Boolean =
    configuration
      .getBoolean(passcodeEnabledKey)
      .getOrElse(throwConfigNotFound(passcodeEnabledKey))
  lazy val passcodeRegime: String =
    configuration
      .getString(passcodeRegimeKey)
      .getOrElse(throwConfigNotFound(passcodeRegimeKey))
  lazy val env: String =
    if (environment.mode.equals(Mode.Test)) "Test"
    else configuration.getString("run.mode").getOrElse("Dev")
  lazy val verificationURL: String =
    configuration
      .getString(s"govuk-tax.$env.url.verification-frontend.redirect")
      .getOrElse("/verification")
  lazy val logoutUrl = s"$verificationURL/otac/logout/$passcodeRegime"

  def loginUrl[A](queryParam: String) =
    s"$verificationURL/otac/login$queryParam"

  def throwConfigNotFound(configKey: String) =
    throw new PasscodeVerificationException(s"The value for the key '$configKey' should be setup in the config file.")

  def addRedirectUrl[A](token: String)(implicit request: Request[A]): Result => Result =
    e =>
      e.addingToSession(SessionKeys.redirect -> buildRedirectUrl(request))
        .addingToSession("otacTokenParam" -> token)

  def buildRedirectUrl[A](req: Request[A]): String =
    if (env != "Prod")
      s"http${if (req.secure) "s" else ""}://${req.host}${req.path}"
    else req.path

  def apply[A](body: Boolean => Future[Result])(
    implicit
    request: Request[A],
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    if (passcodeEnabled) {
      request.session
        .get(SessionKeys.otacToken)
        .fold(request.getQueryString(tokenParam) match {
          case Some(token) => {
            val queryParam = s"?$tokenParam=$token"
            Future
              .successful(Redirect(loginUrl(queryParam))) map addRedirectUrl(token)(request)
          }
          case _ => body(false)
        }) { otacToken =>
          otacAuthConnector
            .authorise(passcodeRegime, headerCarrier, Option(otacToken))
            .flatMap {
              case Authorised => body(true)
              case _          => body(false)
            }
        }
    } else {
      body(true)
    }
}
