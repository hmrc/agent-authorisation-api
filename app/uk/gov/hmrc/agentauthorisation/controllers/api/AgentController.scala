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
import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.mvc.Results._
import play.api.mvc.{ Action, AnyContent, Request, Result }
import uk.gov.hmrc.agentauthorisation.auth.AuthActions
import uk.gov.hmrc.agentauthorisation.models.AgentInvitation
import uk.gov.hmrc.agentauthorisation.services.{ InvitationService, _ }
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.agentauthorisation.models.Services.{mtdVat, mtdIt}

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class AgentController @Inject() (invitationService: InvitationService, val authConnector: AuthConnector) extends BaseController with AuthActions {

  import AgentController._

  def createInvitationApi(arn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      val invitationResponse = request.body.asJson match {
        case Some(json) => json.as[AgentInvitation]
        case None => AgentInvitation("", "", "", "")
      }
      invitationResponse match {
        case ItsaInvitation(invitation) =>
          validateNino(invitation) {
            checkPostcodeAndCreate(arn, invitation)
          }
        case _ => Future successful UnsupportedService
      }
    }
  }

  private def validateNino(agentInvitation: AgentInvitation)(body: => Future[Result]): Future[Result] =
    if(Nino.isValid(agentInvitation.clientId)) body else Future successful InvalidItsaNino

  private def checkPostcodeAndCreate(arn: Arn, agentInvitation: AgentInvitation)(implicit hc: HeaderCarrier): Future[Result] = {
    if(agentInvitation.knownFact.matches(postcodeRegex)){
      for {
        hasPostCode <- invitationService.checkPostcodeMatches(Nino(agentInvitation.clientId), agentInvitation.knownFact)
        result <- hasPostCode match {
          case Some(true) => invitationService.createInvitationService(arn, agentInvitation).flatMap {
            case Some(url) =>
              //TODO Construct location with GET Endpoint
              Future successful NoContent.withHeaders(LOCATION -> url)
            case None =>
              //TODO What happens here?
              Future successful BadRequest
          }
          case Some(false) => Future successful PostcodeDoesNotMatch
          case _ => Future successful ClientRegistrationNotFound
        }
      } yield result
    } else {
      Future successful PostcodeFormatInvalid
    }
  }
}

object AgentController {

  private val postcodeRegex = "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$|BFPO\\s?[0-9]{1,5}$"


  object ItsaInvitation {
    def unapply(arg: AgentInvitation): Option[AgentInvitation] =
      arg match {
        case AgentInvitation("MTD-IT", "ni", _, _) =>
          Some(arg.copy(service = "HMRC-MTD-IT"))
        case _ => None
      }
  }

  object VatInvitation {
    def unapply(arg: AgentInvitation): Option[AgentInvitation] =
      arg match {
        case AgentInvitation("MTD-VAT", "vrn", _, _) =>
          Some(arg.copy())
      }
  }

}
