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

package uk.gov.hmrc.agentauthorisation.controllers.api.agent

import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentauthorisation.auth.AuthActions
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models.{ApiErrorResponse, InvitationDetails}
import uk.gov.hmrc.agentauthorisation.services.InvitationService
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GetInvitationsController @Inject() (
  invitationService: InvitationService,
  val authConnector: AuthConnector,
  cc: ControllerComponents
)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends BackendController(cc) with AuthActions {

  val getRequestsShowLastDays = appConfig.showLastDays

  def getInvitationApi(givenArn: Arn, invitationId: InvitationId): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthorisedAsAgent { arn =>
        implicit val loggedInArn: Arn = arn
        validateArnInRequest(givenArn) {
          invitationService
            .getInvitation(arn, invitationId)
            .map {
              case Right(invitationDetails) =>
                Ok(toJson(invitationDetails)(InvitationDetails.writesForStatus(arn)))
              case Left(errorResponse: ApiErrorResponse) =>
                errorResponse.toResult
            }
        }
      }
    }

}
