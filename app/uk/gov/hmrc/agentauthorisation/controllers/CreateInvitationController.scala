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

package uk.gov.hmrc.agentauthorisation.controllers

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.agentauthorisation.actions.{AuthorisedAgentRequest, VersionedAgentAction}
import uk.gov.hmrc.agentauthorisation.controllers.routes
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.models.v3.CreateInvitationV3Response
import uk.gov.hmrc.agentauthorisation.services.{CreateInvitationService, ValidateClientAccessDataService}
import uk.gov.hmrc.agentauthorisation.services.v3.CreateInvitationV3Service
import uk.gov.hmrc.agentauthorisation.models.Arn
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateInvitationController @Inject() (
  createInvitationService: CreateInvitationService,
  createInvitationV3Service: CreateInvitationV3Service,
  validateClientAccessDataService: ValidateClientAccessDataService,
  cc: ControllerComponents,
  versionedAgentAction: VersionedAgentAction
)(using val ec: ExecutionContext)
    extends BackendController(cc):

  def createInvitation(givenArn: Arn): Action[AnyContent] =
    versionedAgentAction(givenArn)(createInvitationV1V2, createInvitationV3)

  private def createInvitationV1V2(request: AuthorisedAgentRequest[AnyContent]): Future[Result] =
    validateClientAccessDataService
      .validateCreateInvitationPayload(request.body.asJson)
      .fold(
        errorResponse => {
          Logger(getClass).warn(s"Payload failed validation: $errorResponse")
          Future successful errorResponse.toResult
        },
        payload =>
          createInvitationService.createInvitation(request.arn, payload)(using request).map {
            case Right(invitationId) =>
              NoContent
                .withHeaders(LOCATION -> routes.GetInvitationsController.getInvitationApi(request.arn, invitationId).url)
            case Left(errorResponse @ DuplicateAuthorisationRequest(invitationId)) =>
              errorResponse.toResult
                .withHeaders(
                  LOCATION -> routes.GetInvitationsController.getInvitationApi(request.arn, invitationId).url
                )
            case Left(errorResponse: ApiErrorResponse) =>
              errorResponse.toResult
          }
      )

  private def createInvitationV3(request: AuthorisedAgentRequest[AnyContent]): Future[Result] =
    createInvitationV3Service.createInvitation(request.arn, request.body.asJson)(using request).map {
      case Right(invitationId) => Created(Json.toJson(CreateInvitationV3Response(invitationId.value)))
      case Left(error)         => error.toResult
    }
