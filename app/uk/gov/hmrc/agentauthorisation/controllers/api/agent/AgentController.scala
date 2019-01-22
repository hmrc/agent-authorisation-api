/*
 * Copyright 2019 HM Revenue & Customs
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
import javax.inject.{Inject, Named, Singleton}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTimeZone, LocalDate}
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.agentauthorisation.audit.AuditService
import uk.gov.hmrc.agentauthorisation.auth.AuthActions
import uk.gov.hmrc.agentauthorisation.connectors.{DesConnector, InvitationsConnector, RelationshipsConnector}
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.agentauthorisation.controllers.api.PasscodeVerification
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.services.InvitationService
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, Vrn}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentController @Inject()(
  @Named("get-requests-show-last-days") val getRequestsShowLastDays: Int,
  invitationsConnector: InvitationsConnector,
  invitationService: InvitationService,
  relationshipsConnector: RelationshipsConnector,
  desConnector: DesConnector,
  auditService: AuditService,
  val authConnector: AuthConnector,
  val withVerifiedPasscode: PasscodeVerification,
  ecp: Provider[ExecutionContext])
    extends BaseController with AuthActions {

  implicit val ec: ExecutionContext = ecp.get

  import AgentController._

  def createInvitationApi(givenArn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { (arn, _) =>
      implicit val loggedInArn: Arn = arn
      forThisAgency(givenArn) {
        request.body.asJson.map(_.validate[AgentInvitationReceived]) match {
          case Some(JsSuccess(ItsaInvitation(invitation), _)) =>
            validateClientType(invitation) {
              validateNino(invitation) {
                checkKnownFactAndCreate(arn, invitation)
              }
            }
          case Some(JsSuccess(VatInvitation(invitation), _)) =>
            validateClientType(invitation) {
              validateVrn(invitation) {
                checkKnownFactAndCreate(arn, invitation)
              }
            }
          case Some(JsSuccess(s, _)) =>
            Logger(getClass).warn(s"Unsupported service received: ${s.service.mkString("[", ",", "]")}")
            Future successful UnsupportedService
          case Some(JsError(errors)) =>
            Logger(getClass).warn(s"Invalid payload: $errors")
            Future successful InvalidPayload
          case None =>
            Logger(getClass).warn(
              s"Unsupported Content-Type, should be application/json but was ${request.contentType}")
            Future successful InvalidPayload
        }
      }
    }
  }

  def getInvitationApi(givenArn: Arn, invitationId: InvitationId): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthorisedAsAgent { (arn, _) =>
        implicit val loggedInArn: Arn = arn
        forThisAgency(givenArn) {
          invitationsConnector
            .getInvitation(arn, invitationId)
            .map {
              case pendingInv @ Some(PendingInvitation(pendingInvitation))
                  if supportedServices.exists(pendingInvitation.service.contains) =>
                val id = pendingInv.get.href.toString.split("/").toStream.last
                val newInvitationUrl =
                  s"${routes.AgentController.getInvitationApi(arn, InvitationId(id)).path()}"
                Ok(
                  toJson(pendingInvitation
                    .copy(href = newInvitationUrl))
                    .as[JsObject])
              case Some(PendingInvitation(pendingInvitation)) =>
                Logger(getClass).warn(s"Service ${pendingInvitation.service} Not Supported")
                UnsupportedService
              case respondedInv @ Some(RespondedInvitation(respondedInvitation))
                  if supportedServices.exists(respondedInvitation.service.contains) =>
                val id = respondedInv.get.href.toString.split("/").toStream.last
                val newInvitationUrl =
                  s"${routes.AgentController.getInvitationApi(arn, InvitationId(id)).path()}"
                Ok(toJson(respondedInvitation.copy(href = newInvitationUrl))
                  .as[JsObject])
              case Some(RespondedInvitation(respondedInvitation)) =>
                Logger(getClass).warn(s"Service ${respondedInvitation.service} Not Supported")
                UnsupportedService
              case _ =>
                Logger(getClass).warn(s"Invitation ${invitationId.value} Not Found")
                InvitationNotFound
            }
        }
      }
    }

  def cancelInvitationApi(givenArn: Arn, invitationId: InvitationId): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthorisedAsAgent { (arn, _) =>
        implicit val loggedInArn: Arn = arn
        forThisAgency(givenArn) {
          invitationsConnector
            .cancelInvitation(arn, invitationId)
            .map {
              case Some(204) =>
                auditService.sendAgentInvitationCancelled(arn, invitationId.value, "Success")
                NoContent
              case Some(404) => InvitationNotFound
              case Some(403) => NoPermissionOnAgency
              case _ =>
                auditService.sendAgentInvitationCancelled(
                  arn,
                  invitationId.value,
                  "Fail",
                  Some(s"INVALID_INVITATION_STATUS"))
                Logger(getClass).warn(
                  s"Invitation Cancellation Failed: cannot transition the current status to Cancelled")
                InvalidInvitationStatus
            }
            .recoverWith {
              case e =>
                auditService.sendAgentInvitationCancelled(
                  arn,
                  invitationId.value,
                  "Fail",
                  Some(s"Request to Cancel Invitation ${invitationId.value} failed due to: ${e.getMessage}"))
                Logger(getClass).warn(s"Invitation Cancellation Failed: ${e.getMessage}")
                Future.failed(e)
            }
        }
      }
    }

  def checkRelationshipApi(givenArn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { (arn, _) =>
      implicit val loggedInAgent: Arn = arn
      forThisAgency(givenArn) {
        val invitationResponse = request.body.asJson match {
          case Some(json) => json.as[AgentInvitationReceived]
          case None       => AgentInvitationReceived(List.empty, "", "", "", "")
        }
        invitationResponse match {
          case ItsaInvitation(invitation) =>
            validateClientType(invitation) {
              validateNino(invitation) {
                checkKnownFactAndRelationship(arn, invitation)
              }
            }
          case VatInvitation(invitation) =>
            validateVrn(invitation) {
              checkKnownFactAndRelationship(arn, invitation)
            }
          case s =>
            Logger(getClass).warn(s"Unsupported service received: ${s.service}")
            Future successful UnsupportedService
        }
      }
    }
  }

  private def checkKnownFactAndRelationship(arn: Arn, agentInvitation: AgentInvitation)(
    implicit
    hc: HeaderCarrier,
    request: Request[_]): Future[Result] =
    if (checkKnownFactValid(agentInvitation)) {
      for {
        hasKnownFact <- checkKnownFactMatches(agentInvitation)
        result <- hasKnownFact match {
                   case Some(true) => checkRelationship(agentInvitation, arn)
                   case Some(false) =>
                     knownFactNotMatchedAudit(agentInvitation, arn, "checkRelationship")
                     Logger(getClass).warn(s"Postcode does not match for ${agentInvitation.service}")
                     Future successful knownFactDoesNotMatch(agentInvitation.service)
                   case _ =>
                     auditService.sendAgentCheckRelationshipStatus(
                       arn,
                       agentInvitation,
                       "Fail",
                       Some("CLIENT_REGISTRATION_NOT_FOUND"))
                     Logger(getClass).warn(s"Client Registration Not Found")
                     Future successful ClientRegistrationNotFound
                 }
      } yield result
    } else {
      Logger(getClass).warn(s"Invalid Format for supplied Known Fact")
      Future successful knownFactFormatInvalid(agentInvitation.service)
    }

  private def checkKnownFactAndCreate(arn: Arn, agentInvitation: AgentInvitation)(
    implicit
    hc: HeaderCarrier,
    request: Request[_]): Future[Result] =
    if (checkKnownFactValid(agentInvitation)) {
      for {
        hasKnownFact <- checkKnownFactMatches(agentInvitation)
        result <- hasKnownFact match {
                   case Some(true) =>
                     invitationService
                       .createInvitation(arn, agentInvitation)
                       .flatMap { invitationUrl =>
                         val id = invitationUrl._2
                           .split("/")
                           .toStream
                           .last
                         auditService.sendAgentInvitationSubmitted(arn, id, agentInvitation, "Success")
                         Future successful NoContent.withHeaders(LOCATION -> invitationUrl._1)
                       }
                       .recoverWith {
                         case e =>
                           Logger(getClass).warn(s"Invitation Creation Failed: ${e.getMessage}")
                           auditService
                             .sendAgentInvitationSubmitted(arn, "", agentInvitation, "Fail", Some(e.getMessage))
                           Future.failed(e)
                       }
                   case Some(false) =>
                     knownFactNotMatchedAudit(agentInvitation, arn, "createInvitation")
                     Future successful knownFactDoesNotMatch(agentInvitation.service)
                   case _ =>
                     auditService.sendAgentInvitationSubmitted(
                       arn,
                       "",
                       agentInvitation,
                       "Fail",
                       Some("CLIENT_REGISTRATION_NOT_FOUND"))
                     Logger(getClass).warn(s"Client Registration Not Found")
                     Future successful ClientRegistrationNotFound
                 }
      } yield result
    } else {
      Logger(getClass).warn(s"Invalid Format for supplied Known Fact")
      Future successful knownFactFormatInvalid(agentInvitation.service)
    }

  private def forThisAgency(requestedArn: Arn)(block: => Future[Result])(
    implicit
    arn: Arn) =
    if (requestedArn != arn) {
      Logger(getClass).warn(s"Requested Arn ${requestedArn.value} does not match to logged in Arn")
      Future successful NoPermissionOnAgency
    } else block

  private def checkKnownFactMatches(agentInvitation: AgentInvitation)(
    implicit
    hc: HeaderCarrier) =
    agentInvitation.service match {
      case "HMRC-MTD-IT" =>
        invitationsConnector.checkPostcodeForClient(Nino(agentInvitation.clientId), agentInvitation.knownFact)
      case _ =>
        invitationsConnector
          .checkVatRegDateForClient(Vrn(agentInvitation.clientId), LocalDate.parse(agentInvitation.knownFact))
    }

  private def knownFactNotMatchedAudit(agentInvitation: AgentInvitation, arn: Arn, usage: String)(
    implicit
    hc: HeaderCarrier,
    request: Request[_]) =
    agentInvitation.service match {
      case "HMRC-MTD-IT" =>
        usage match {
          case "createInvitation" =>
            auditService.sendAgentInvitationSubmitted(arn, "", agentInvitation, "Fail", Some("POSTCODE_DOES_NOT_MATCH"))
          case "checkRelationship" =>
            auditService.sendAgentCheckRelationshipStatus(arn, agentInvitation, "Fail", Some("POSTCODE_DOES_NOT_MATCH"))
        }
      case "HMRC-MTD-VAT" =>
        usage match {
          case "createInvitation" =>
            auditService
              .sendAgentInvitationSubmitted(arn, "", agentInvitation, "Fail", Some("VAT_REG_DATE_DOES_NOT_MATCH"))
          case "checkRelationship" =>
            auditService
              .sendAgentCheckRelationshipStatus(arn, agentInvitation, "Fail", Some("VAT_REG_DATE_DOES_NOT_MATCH"))
        }
    }

  private def checkRelationship(agentInvitation: AgentInvitation, arn: Arn)(
    implicit
    hc: HeaderCarrier,
    request: Request[_]) =
    agentInvitation.service match {
      case "HMRC-MTD-IT" => {
        val res = for {
          mtdItId <- desConnector.getMtdIdFor(Nino(agentInvitation.clientId))
          result <- mtdItId match {
                     case Right(id) =>
                       relationshipsConnector.checkItsaRelationship(arn, id)
                     case Left(_) => Future successful false
                   }
        } yield result
        res.map {
          case true =>
            auditService.sendAgentCheckRelationshipStatus(arn, agentInvitation, "Success")
            NoContent
          case false =>
            auditService
              .sendAgentCheckRelationshipStatus(arn, agentInvitation, "Fail", Some("ITSA_RELATIONSHIP_NOT_FOUND"))
            Logger(getClass).warn(s"No ITSA Relationship Found")
            RelationshipNotFound
        }
      }
      case "HMRC-MTD-VAT" => {
        relationshipsConnector
          .checkVatRelationship(arn, Vrn(agentInvitation.clientId))
          .map {
            case true =>
              auditService.sendAgentCheckRelationshipStatus(arn, agentInvitation, "Success")
              NoContent
            case false =>
              auditService
                .sendAgentCheckRelationshipStatus(arn, agentInvitation, "Fail", Some("VAT_RELATIONSHIP_NOT_FOUND"))
              Logger(getClass).warn(s"No VAT Relationship Found")
              RelationshipNotFound
          }
      }
    }

  def getInvitationsApi(givenArn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { (arn, _) =>
      implicit val loggedInArn: Arn = arn
      forThisAgency(givenArn) {
        val previousDate =
          LocalDate.now(DateTimeZone.UTC).minusDays(getRequestsShowLastDays)
        invitationService
          .getAllInvitations(arn, previousDate)
          .map(invitations => {
            invitations
              .collect {
                case si if supportedServices.contains(si.service) => si
              }
              .map {
                case pendingInv @ PendingInvitation(_) =>
                  val id = pendingInv.href.toString.split("/").toStream.last
                  val newInvitationUrl =
                    s"${routes.AgentController.getInvitationApi(arn, InvitationId(id)).path()}"
                  PendingOrRespondedInvitation(
                    Links(newInvitationUrl),
                    pendingInv.created,
                    pendingInv.arn,
                    List(pendingInv.service),
                    pendingInv.status,
                    Some(pendingInv.expiresOn),
                    pendingInv.clientActionUrl,
                    None
                  )

                case respondedInv @ RespondedInvitation(_) =>
                  val id = respondedInv.href.toString.split("/").toStream.last
                  val newInvitationUrl =
                    s"${routes.AgentController.getInvitationApi(arn, InvitationId(id)).path()}"
                  PendingOrRespondedInvitation(
                    Links(newInvitationUrl),
                    respondedInv.created,
                    respondedInv.arn,
                    List(respondedInv.service),
                    respondedInv.status,
                    None,
                    None,
                    Some(respondedInv.updated))

              }
          })
          .map {
            case s if s.isEmpty => NoContent
            case s              => Ok(Json.toJson(s))
          }
      }
    }
  }

}

object AgentController {

  private val postcodeRegex =
    "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$|BFPO\\s?[0-9]{1,5}$"

  private val supportedServices = Seq("MTD-IT", "MTD-VAT")

  val personal = "personal"
  val business = "business"

  val supportedClientTypes = Map("HMRC-MTD-IT" -> Seq("personal"), "HMRC-MTD-VAT" -> Seq("personal", "business"))

  private def validateClientType(agentInvitation: AgentInvitation)(body: => Future[Result]): Future[Result] =
    if (supportedClientTypes(agentInvitation.service).contains(agentInvitation.clientType)) body
    else {
      Logger(getClass).warn(s"Unsupported Client Type")
      Future successful UnsupportedClientType
    }

  private def validateNino(agentInvitation: AgentInvitation)(body: => Future[Result]): Future[Result] =
    if (Nino.isValid(agentInvitation.clientId)) body
    else if (Vrn.isValid(agentInvitation.clientId)) {
      Logger(getClass).warn(s"Client Id does not match service")
      Future successful ClientIdDoesNotMatchService
    } else {
      Logger(getClass).warn(s"Invalid Nino provided for ITSA")
      Future successful ClientIdInvalidFormat
    }

  private def validateVrn(agentInvitation: AgentInvitation)(body: => Future[Result]): Future[Result] =
    if (Vrn.isValid(agentInvitation.clientId)) body
    else if (Nino.isValid(agentInvitation.clientId)) {
      Logger(getClass).warn(s"Client Id does not match service")
      Future successful ClientIdDoesNotMatchService
    } else {
      Logger(getClass).warn(s"Invalid Vrn provided for VAT")
      Future successful ClientIdInvalidFormat
    }

  def validateDate(value: String): Boolean =
    if (parseDate(value)) true else false

  val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  def parseDate(date: String): Boolean =
    try {
      dateTimeFormat.parseDateTime(date)
      true
    } catch {
      case _: Throwable => false
    }

  private def checkKnownFactValid(agentInvitation: AgentInvitation): Boolean =
    agentInvitation.service match {
      case "HMRC-MTD-IT" => {
        agentInvitation.knownFact.matches(postcodeRegex)
      }
      case "HMRC-MTD-VAT" => validateDate(agentInvitation.knownFact)
    }

  private def knownFactDoesNotMatch(service: String) =
    service match {
      case "HMRC-MTD-IT"  => PostcodeDoesNotMatch
      case "HMRC-MTD-VAT" => VatRegDateDoesNotMatch
    }

  private def knownFactFormatInvalid(service: String) =
    service match {
      case "HMRC-MTD-IT"  => PostcodeFormatInvalid
      case "HMRC-MTD-VAT" => VatRegDateFormatInvalid
    }

  object ItsaInvitation {
    def unapply(arg: AgentInvitationReceived): Option[AgentInvitation] =
      arg match {
        case AgentInvitationReceived(List("MTD-IT"), "personal", "ni", _, _) =>
          Some(AgentInvitation("HMRC-MTD-IT", "personal", arg.clientIdType, arg.clientId, arg.knownFact))
        case _ => None
      }
  }

  object VatInvitation {
    def unapply(arg: AgentInvitationReceived): Option[AgentInvitation] =
      arg match {
        case AgentInvitationReceived(List("MTD-VAT"), _, "vrn", _, _) =>
          Some(AgentInvitation("HMRC-MTD-VAT", arg.clientType, arg.clientIdType, arg.clientId, arg.knownFact))
        case _ => None
      }
  }

}
