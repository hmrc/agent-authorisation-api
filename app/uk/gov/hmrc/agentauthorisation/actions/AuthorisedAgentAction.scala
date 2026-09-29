/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.actions

import play.api.Logger
import play.api.mvc.{ActionRefiner, ControllerComponents, Request, Result, WrappedRequest}
import uk.gov.hmrc.agentauthorisation.auth.AuthActions
import uk.gov.hmrc.agentauthorisation.models.{ApiVersion, Arn, NoPermissionOnAgency}
import uk.gov.hmrc.auth.core.AuthConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

final class AuthorisedAgentRequest[A](
  val arn: Arn,
  val apiVersion: ApiVersion,
  request: Request[A]
) extends WrappedRequest[A](request)

@Singleton
class AuthorisedAgentAction @Inject() (
  override val authConnector: AuthConnector,
  controllerComponents: ControllerComponents
) extends AuthActions:

  private given ExecutionContext = controllerComponents.executionContext
  private val logger = Logger(getClass)

  def apply(requestedArn: Arn): ActionRefiner[ApiVersionRequest, AuthorisedAgentRequest] =
    new ActionRefiner[ApiVersionRequest, AuthorisedAgentRequest]:
      override protected def refine[A](
        request: ApiVersionRequest[A]
      ): Future[Either[Result, AuthorisedAgentRequest[A]]] =
        given Request[A] = request

        authorisedAgentArn.map {
          case Right(authenticatedArn) if authenticatedArn == requestedArn =>
            Right(new AuthorisedAgentRequest(authenticatedArn, request.apiVersion, request))
          case Right(authenticatedArn) =>
            logger.warn(
              s"Requested Arn ${requestedArn.value} does not match to logged in Arn ${authenticatedArn.value}"
            )
            Left(NoPermissionOnAgency.toResult)
          case Left(error) => Left(error.toResult)
        }

      override protected def executionContext: ExecutionContext = controllerComponents.executionContext
