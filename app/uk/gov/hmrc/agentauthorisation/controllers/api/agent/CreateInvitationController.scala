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

package uk.gov.hmrc.agentauthorisation.controllers.api.agent

import com.google.inject.Provider
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.agentauthorisation.auth.AuthActions
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.services.CreateInvitationService
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateInvitationController @Inject() (
  createInvitationService: CreateInvitationService,
  val authConnector: AuthConnector,
  ecp: Provider[ExecutionContext],
  cc: ControllerComponents
) extends BackendController(cc) with AuthActions {

  implicit val ec: ExecutionContext = ecp.get

  def createInvitation(givenArn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      implicit val loggedInArn: Arn = arn
      validateArnInRequest(givenArn) {
        createInvitationService
          .validatePayload(request.body.asJson)
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
                    .withHeaders(LOCATION -> routes.AgentController.getInvitationApi(arn, invitationId).url)
                case Left(errorResponse @ DuplicateAuthorisationRequest(invitationId)) =>
                  errorResponse.toResult
                    .withHeaders(
                      LOCATION -> routes.AgentController.getInvitationApi(arn, invitationId).url
                    )
                case Left(errorResponse: ApiErrorResponse) =>
                  errorResponse.toResult
              }
          )
      }
    }
  }

  // TODO: Move this into auth actions after all endpoints refactored
  private def validateArnInRequest(requestedArn: Arn)(block: => Future[Result])(implicit arn: Arn): Future[Result] =
    if (requestedArn != arn) {
      Logger(getClass).warn(s"Requested Arn ${requestedArn.value} does not match to logged in Arn")
      Future successful NoPermissionOnAgency.toResult
    } else block
}
