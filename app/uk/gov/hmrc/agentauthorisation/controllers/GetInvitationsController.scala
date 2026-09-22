/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.controllers

import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentauthorisation.actions.{ApiVersionAction, AuthorisedAgentAction}
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models.{AllInvitationDetails, ApiErrorResponse, SingleInvitationDetails}
import uk.gov.hmrc.agentauthorisation.services.GetInvitationsService
import uk.gov.hmrc.agentauthorisation.models.{Arn, InvitationId}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GetInvitationsController @Inject() (
  getInvitationsService: GetInvitationsService,
  cc: ControllerComponents,
  appConfig: AppConfig,
  apiVersionAction: ApiVersionAction,
  authorisedAgentAction: AuthorisedAgentAction
)(using ec: ExecutionContext)
    extends BackendController(cc):

  def getInvitationApi(givenArn: Arn, invitationId: InvitationId): Action[AnyContent] =
    cc.actionBuilder.andThen(apiVersionAction).andThen(authorisedAgentAction(givenArn)).async { request =>
      getInvitationsService
        .getInvitation(request.arn, invitationId)(using request)
        .map {
          case Right(invitationDetails) =>
            Ok(toJson(invitationDetails)(using SingleInvitationDetails.apiWrites(request.arn, appConfig.acrfExternalUrl)))
          case Left(errorResponse: ApiErrorResponse) =>
            errorResponse.toResult
        }
    }

  def getInvitationsApi(givenArn: Arn): Action[AnyContent] =
    cc.actionBuilder.andThen(apiVersionAction).andThen(authorisedAgentAction(givenArn)).async { request =>
      getInvitationsService
        .getAllInvitations(request.arn)(using request)
        .map {
          case Right(AllInvitationDetails(_, Nil)) =>
            NoContent
          case Right(invitationDetails) =>
            Ok(toJson(invitationDetails)(using AllInvitationDetails.apiWrites(request.arn, appConfig.acrfExternalUrl)))
          case Left(errorResponse: ApiErrorResponse) =>
            errorResponse.toResult
        }
    }
