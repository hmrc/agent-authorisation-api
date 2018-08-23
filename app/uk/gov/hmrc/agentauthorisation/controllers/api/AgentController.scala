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

package uk.gov.hmrc.agentauthorisation.controllers.api

import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, AnyContent }
import uk.gov.hmrc.agentauthorisation.auth.AuthActions
import uk.gov.hmrc.agentauthorisation.models.AgentInvitation
import uk.gov.hmrc.agentauthorisation.services.{ InvitationService, _ }
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

@Singleton
class AgentController @Inject() (invitationService: InvitationService, val authConnector: AuthConnector) extends BaseController with AuthActions {

  def createInvitationApi(arn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      val invitation = request.body.asJson match {
        case Some(json) => json.as[AgentInvitation]
        case None => AgentInvitation(List.empty, "", "", "")
      }
      invitationService.createInvitationService(arn, invitation).map {
        case Some(url) => NoContent.withHeaders(LOCATION -> url)
        case None => BadRequest
      }
    }
  }

}
