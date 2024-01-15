/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.Logger
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.agentauthorisation.controllers.api.errors.ErrorResponse._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends AuthorisedFunctions {

  private val affinityGroupAllEnrolls: Retrieval[Option[AffinityGroup] ~ Enrolments] = affinityGroup and allEnrolments

  private def isAgent(group: AffinityGroup): Boolean =
    group.toString.contains("Agent")

  private def extractEnrolmentData(enrolls: Set[Enrolment], enrolKey: String, enrolId: String): Option[String] =
    enrolls
      .find(_.key == enrolKey)
      .flatMap(_.getIdentifier(enrolId))
      .map(_.value)

  protected def withEnrolledAsAgent[A](
    body: String => Future[Result]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(affinityGroupAllEnrolls) {
        case Some(affinity) ~ allEnrols =>
          (
            isAgent(affinity),
            extractEnrolmentData(allEnrols.enrolments, "HMRC-AS-AGENT", "AgentReferenceNumber")
          ) match {
            case (true, Some(arn)) => body(arn)
            case (true, None) =>
              Logger(getClass).warn(
                s"Logged in user has Affinity Group: Agent but does not have Enrolment: HMRC-AS-AGENT"
              )
              Future successful AgentNotSubscribed
            case _ =>
              Logger(getClass).warn(s"Logged in user does not have Affinity Group: Agent. Discovered: $affinity")
              Future successful NotAnAgent
          }
        case _ =>
          Logger(getClass).warn(s"User Attempted to Login with Invalid Credentials")
          Future successful NotAnAgent
      }

  protected def withAuthorisedAsAgent[A](
    body: Arn => Future[Result]
  )(implicit ec: ExecutionContext, request: Request[_]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    withEnrolledAsAgent { arn =>
      body(Arn(arn))
    } recoverWith {
      case _: InsufficientEnrolments =>
        Logger(getClass).warn(s"User has Insufficient Enrolments to Login")
        Future successful NotAnAgent
      case e: AuthorisationException =>
        Logger(getClass).warn(s"User has Missing Bearer Token in Header or: $e")
        Future successful standardUnauthorised
    }
  }
}
