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
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.agentauthorisation.actions.{AuthorisedAgentRequest, VersionedAgentAction}
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models.{
  AllInvitationDetails,
  ApiErrorResponse,
  Arn,
  InvitationId,
  SingleInvitationDetails
}
import uk.gov.hmrc.agentauthorisation.services.GetInvitationsService
import uk.gov.hmrc.agentauthorisation.services.v3.{GetInvitationByIdV3Service, GetInvitationsV3Service}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetInvitationsController @Inject() (
  getInvitationsService: GetInvitationsService,
  getInvitationByIdV3Service: GetInvitationByIdV3Service,
  getInvitationsV3Service: GetInvitationsV3Service,
  cc: ControllerComponents,
  appConfig: AppConfig,
  versionedAgentAction: VersionedAgentAction
)(using ec: ExecutionContext)
    extends BackendController(cc):

  def getInvitationApi(givenArn: Arn, invitationId: InvitationId): Action[AnyContent] =
    versionedAgentAction(givenArn)(
      getInvitationV1V2(invitationId),
      getInvitationV3(invitationId)
    )

  private def getInvitationV1V2(
    invitationId: InvitationId
  )(request: AuthorisedAgentRequest[AnyContent]): Future[Result] =
    getInvitationsService
      .getInvitation(request.arn, invitationId)(using request)
      .map {
        case Right(invitationDetails) =>
          Ok(toJson(invitationDetails)(using SingleInvitationDetails.apiWrites(request.arn, appConfig.acrfExternalUrl)))
        case Left(errorResponse: ApiErrorResponse) =>
          errorResponse.toResult
      }

  private def getInvitationV3(
    invitationId: InvitationId
  )(request: AuthorisedAgentRequest[AnyContent]): Future[Result] =
    getInvitationByIdV3Service.getInvitation(request.arn, invitationId)(using request).map:
      case Right(invitation) => Ok(toJson(invitation))
      case Left(error)       => error.toResult

  def getInvitationsApi(givenArn: Arn): Action[AnyContent] =
    versionedAgentAction(givenArn)(getInvitationsV1V2, getInvitationsV3)

  private def getInvitationsV1V2(request: AuthorisedAgentRequest[AnyContent]): Future[Result] =
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

  private def getInvitationsV3(request: AuthorisedAgentRequest[AnyContent]): Future[Result] =
    getInvitationsV3Service.getInvitations(request.arn)(using request).map {
      case Right(invitations) => Ok(toJson(invitations))
      case Left(error)        => error.toResult
    }
