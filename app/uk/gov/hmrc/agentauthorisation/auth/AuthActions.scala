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

package uk.gov.hmrc.agentauthorisation.auth

import play.api.mvc.Results._
import play.api.mvc.{ Request, Result }
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.authorisedEnrolments
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ ExecutionContext, Future }

trait AuthActions extends AuthorisedFunctions {

  private def getEnrolmentValue(enrolments: Enrolments, serviceName: String, identifierKey: String) =
    for {
      enrolment <- enrolments.getEnrolment(serviceName)
      identifier <- enrolment.getIdentifier(identifierKey)
    } yield identifier.value

  protected def withEnrolledAsAgent[A](body: Option[String] => Future[Result])(
    implicit
    request: Request[A],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    authorised(
      Enrolment("HMRC-AS-AGENT")
        and AuthProviders(GovernmentGateway))
      .retrieve(authorisedEnrolments) { enrolments =>
        val id = getEnrolmentValue(enrolments, "HMRC-AS-AGENT", "AgentReferenceNumber")
        body(id)
      }

  protected def withAuthorisedAsAgent[A](body: Arn => Future[Result])(
    implicit
    request: Request[A],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    withEnrolledAsAgent {
      case Some(arn) =>
        body(Arn(arn))
      case None => Future.failed(InsufficientEnrolments("AgentReferenceNumber identifier not found"))
    } recoverWith {
      case _: InsufficientEnrolments => Future successful Forbidden
    }

}
