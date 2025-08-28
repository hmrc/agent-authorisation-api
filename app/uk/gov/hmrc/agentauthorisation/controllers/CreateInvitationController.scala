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
import play.api.mvc._
import uk.gov.hmrc.agentauthorisation.auth.AuthActions
import uk.gov.hmrc.agentauthorisation.controllers.routes
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.services.{CreateInvitationService, ValidateClientAccessDataService}
import uk.gov.hmrc.agentauthorisation.models.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateInvitationController @Inject() (
  createInvitationService: CreateInvitationService,
  validateClientAccessDataService: ValidateClientAccessDataService,
  val authConnector: AuthConnector,
  cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BackendController(cc) with AuthActions {

  def createInvitation(givenArn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      implicit val loggedInArn: Arn = arn
      validateArnInRequest(givenArn) {
        validateClientAccessDataService
          .validateCreateInvitationPayload(request.body.asJson)
          .fold(
            errorResponse => {
              Logger(getClass).warn(s"Payload failed validation: $errorResponse")
              Future successful errorResponse.toResult
            },
            payload =>
              for {
                result <- createInvitationService.createInvitation(arn, payload)
              } yield result match {
                case Right(invitationId) =>
                  NoContent
                    .withHeaders(LOCATION -> routes.GetInvitationsController.getInvitationApi(arn, invitationId).url)
                case Left(errorResponse @ DuplicateAuthorisationRequest(invitationId)) =>
                  errorResponse.toResult
                    .withHeaders(
                      LOCATION -> routes.GetInvitationsController.getInvitationApi(arn, invitationId).url
                    )
                case Left(errorResponse: ApiErrorResponse) =>
                  errorResponse.toResult
              }
          )
      }
    }
  }
}
