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

import javax.inject.{ Inject, Named, Singleton }
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.mvc.{ Action, AnyContent, Result }
import uk.gov.hmrc.agentauthorisation.auth.AuthActions
import uk.gov.hmrc.agentauthorisation.models.AgentInvitation
import uk.gov.hmrc.agentauthorisation.services.{ InvitationService, _ }
import uk.gov.hmrc.agentmtdidentifiers.model.{ Arn, InvitationId, Vrn }
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class AgentController @Inject() (
  invitationService: InvitationService,
  val authConnector: AuthConnector,
  val withVerifiedPasscode: PasscodeVerification) extends BaseController with AuthActions {

  import AgentController._

  def createInvitationApi(givenArn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { (arn, _) =>
      implicit val loggedInArn: Arn = arn
      forThisAgency(givenArn) {
        val invitationResponse = request.body.asJson match {
          case Some(json) => json.as[AgentInvitation]
          case None => AgentInvitation("", "", "", "")
        }
        invitationResponse match {
          case ItsaInvitation(invitation) =>
            validateNino(invitation) {
              checkKnownFactAndCreate(arn, invitation)
            }
          case VatInvitation(invitation) =>
            validateVrn(invitation) {
              checkKnownFactAndCreate(arn, invitation)
            }
          case _ => Future successful UnsupportedService
        }
      }
    }
  }

  def getInvitationApi(arn: Arn, invitationId: InvitationId): Action[AnyContent] = Action.async { implicit request =>
    Future successful NotImplemented
  }

  private def checkKnownFactAndCreate(arn: Arn, agentInvitation: AgentInvitation)(implicit hc: HeaderCarrier): Future[Result] = {
    if (checkKnownFactValid(agentInvitation)) {
      for {
        hasKnownFact <- checkKnownFactMatches(agentInvitation)
        result <- hasKnownFact match {
          case Some(true) =>
            invitationService.createInvitationService(arn, agentInvitation).flatMap { invitationUrl =>
              val id = invitationUrl.toString.split("/").toStream.last
              val newInvitationUrl = s"${routes.AgentController.getInvitationApi(arn, InvitationId(id)).path()}"
              Future successful NoContent.withHeaders(LOCATION -> newInvitationUrl)
            }.recoverWith {
              case e =>
                Logger(getClass).warn(s"Invitation Creation Failed: ${e.getMessage}")
                Future.failed(e)
            }
          case Some(false) => Future successful knownFactDoesNotMatch(agentInvitation.service)
          case _ => Future successful ClientRegistrationNotFound
        }
      } yield result
    } else {
      Future successful knownFactFormatInvalid(agentInvitation.service)
    }
  }

  private def forThisAgency(requestedArn: Arn)(block: => Future[Result])(implicit arn: Arn) =
    if (requestedArn != arn)
      Future successful NoPermissionOnAgency
    else block

  private def checkKnownFactMatches(agentInvitation: AgentInvitation)(implicit hc: HeaderCarrier) = {
    agentInvitation.service match {
      case "HMRC-MTD-IT" => invitationService.checkPostcodeMatches(Nino(agentInvitation.clientId), agentInvitation.knownFact)
      case _ => invitationService.checkVatRegDateMatches(Vrn(agentInvitation.clientId), LocalDate.parse(agentInvitation.knownFact))
    }
  }
}

object AgentController {

  private val postcodeRegex = "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$|BFPO\\s?[0-9]{1,5}$"

  private def validateNino(agentInvitation: AgentInvitation)(body: => Future[Result]): Future[Result] =
    if (Nino.isValid(agentInvitation.clientId)) body else Future successful InvalidItsaNino

  private def validateVrn(agentInvitation: AgentInvitation)(body: => Future[Result]): Future[Result] =
    if (Vrn.isValid(agentInvitation.clientId)) body else Future successful InvalidVatVrn

  def validateDate(value: String): Boolean = if (parseDate(value)) true else false

  val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  def parseDate(date: String): Boolean =
    try {
      dateTimeFormat.parseDateTime(date)
      true
    } catch {
      case _: Throwable => false
    }

  private def checkKnownFactValid(agentInvitation: AgentInvitation): Boolean = {
    agentInvitation.service match {
      case "HMRC-MTD-IT" => {
        agentInvitation.knownFact.matches(postcodeRegex)
      }
      case "HMRC-MTD-VAT" => {
        validateDate(agentInvitation.knownFact)
      }
      case _ => {
        false
      }
    }
  }

  private def knownFactDoesNotMatch(service: String) = {
    service match {
      case "HMRC-MTD-IT" => PostcodeDoesNotMatch
      case "HMRC-MTD-VAT" => VatRegDateDoesNotMatch
    }
  }

  private def knownFactFormatInvalid(service: String) = {
    service match {
      case "HMRC-MTD-IT" => PostcodeFormatInvalid
      case "HMRC-MTD-VAT" => VatRegDateFormatInvalid
    }
  }

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
          Some(arg.copy(service = "HMRC-MTD-VAT"))
        case _ => None
      }
  }

}
