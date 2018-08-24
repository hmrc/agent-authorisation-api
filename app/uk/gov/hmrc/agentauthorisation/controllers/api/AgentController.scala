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
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults.{ PostcodeDoesNotMatch, postcodeFormatInvalid }
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class AgentController @Inject() (invitationService: InvitationService, val authConnector: AuthConnector) extends BaseController with AuthActions {

  private val postcodeRegex = "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$|BFPO\\s?[0-9]{1,5}$"

  def createInvitationApi(arn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      val invitation = request.body.asJson match {
        case Some(json) => json.as[AgentInvitation]
        case None => AgentInvitation("", "", "", "")
      }

      //      val error: Result = PostcodeDoesNotMatch
      //      val result1: Status = BadRequest
      //      val result2: Result = NoContent.withHeaders("" -> "")

      invitation match {
        case AgentInvitation(_, _, _, postcode) if postcode.matches(postcodeRegex) =>
          checkPostcode(arn, invitation)
        //          for {
        //            hasPostcode <- invitationService.checkPostcodeMatches(Nino(invitation.clientId), invitation.knownFact)
        //            result <- hasPostcode match {
        //              case Some(true) =>
        //                invitationService.createInvitationService(arn, invitation).map {
        //                  case Some(url) => NoContent.withHeaders(LOCATION -> url)
        //                  case None => BadRequest
        //                }
        //              case Some(false) => PostcodeDoesNotMatch
        //            }
        //          } yield result
        case AgentInvitation(_, _, _, _) => Future successful postcodeFormatInvalid("The postcode is an invalid format")
      }
    }
  }

  private def checkPostcode(arn: Arn, agentInvitation: AgentInvitation)(implicit hc: HeaderCarrier): Future[Result] = {
    invitationService.checkPostcodeMatches(Nino(agentInvitation.clientId), agentInvitation.knownFact).flatMap {
      case Some(true) => invitationService.createInvitationService(arn, agentInvitation).flatMap {
        case Some(url) => Future successful NoContent.withHeaders(LOCATION -> url)
        case None => Future successful BadRequest
      }
      case Some(false) => Future successful PostcodeDoesNotMatch
      case _ => Future successful Forbidden
    }
  }
}
//
//object AgentController {
//
//
//
//  object CompleteItsaInvitation {
//    def unapply(arg: AgentInvitation): Option[] =
//      arg match {
//        case AgentInvitation("HMRC-MTD-IT", "ni", clientId, knownFact)
//          if Nino.isValid(clientId) && knownFact.matches(postcodeRegex) =>
//      }
//  }
//
//}
