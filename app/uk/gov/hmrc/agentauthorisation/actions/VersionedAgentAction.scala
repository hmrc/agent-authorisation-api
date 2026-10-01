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

import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import play.api.mvc.Results.NotAcceptable
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models.{ApiVersion, Arn, StandardNotFound}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class VersionedAgentAction @Inject() (
  controllerComponents: ControllerComponents,
  apiVersionAction: ApiVersionAction,
  authorisedAgentAction: AuthorisedAgentAction,
  appConfig: AppConfig
):

  private def versionWarning(requires: ApiVersion) = s"invalid client version, this endpoint requires one of [$requires]"

  def apply(requestedArn: Arn)(
    v1V2: AuthorisedAgentRequest[AnyContent] => Future[Result],
    v3: AuthorisedAgentRequest[AnyContent] => Future[Result]
  ): Action[AnyContent] =
    controllerComponents.actionBuilder
      .andThen(apiVersionAction)
      .andThen(authorisedAgentAction(requestedArn))
      .async: request =>
        request.apiVersion match
          case ApiVersion.V1V2 => v1V2(request)
          case ApiVersion.V3   => v3(request)

  def v3Only(requestedArn: Arn)(
    v3: AuthorisedAgentRequest[AnyContent] => Future[Result]
  ): Action[AnyContent] =
    controllerComponents.actionBuilder
      .andThen(apiVersionAction)
      .async: request =>
        request.apiVersion match
          case ApiVersion.V1V2 if appConfig.v3Enabled => Future.successful(NotAcceptable(versionWarning(ApiVersion.V3)))
          case ApiVersion.V1V2                        => Future.successful(StandardNotFound.toResult)
          case ApiVersion.V3                          => authorisedAgentAction(requestedArn).invokeBlock(request, v3)

  def v1V2Only(requestedArn: Arn)(
    v1V2: AuthorisedAgentRequest[AnyContent] => Future[Result]
  ): Action[AnyContent] =
    controllerComponents.actionBuilder
      .andThen(apiVersionAction)
      .async: request =>
        request.apiVersion match
          case ApiVersion.V3 => Future.successful(NotAcceptable(versionWarning(ApiVersion.V1V2)))
          case ApiVersion.V1V2 => authorisedAgentAction(requestedArn).invokeBlock(request, v1V2)
