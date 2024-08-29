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
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.agentauthorisation.audit.AuditService
import uk.gov.hmrc.agentauthorisation.auth.AuthActions
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.connectors.{InvitationsConnector, RelationshipsConnector}
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.agentauthorisation.models.ClientType.{business, personal}
import uk.gov.hmrc.agentauthorisation.models.Service.{Itsa, Vat}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.services.{InvitationService, PlatformAnalyticsService, RelationshipService}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, Vrn}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.agentmtdidentifiers.model.{Service => MtdServie}

@Singleton
class AgentController @Inject() (
  invitationsConnector: InvitationsConnector,
  invitationService: InvitationService,
  relationshipsConnector: RelationshipsConnector,
  relationshipService: RelationshipService,
  auditService: AuditService,
  platformAnalyticsService: PlatformAnalyticsService,
  val authConnector: AuthConnector,
  ecp: Provider[ExecutionContext],
  cc: ControllerComponents,
  appConfig: AppConfig
) extends BackendController(cc) with AuthActions {

  implicit val ec: ExecutionContext = ecp.get

  val getRequestsShowLastDays = appConfig.showLastDays

  import AgentController._

  def createInvitationApi(givenArn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      implicit val loggedInArn: Arn = arn
      forThisAgency(givenArn) {
        request.body.asJson.map(_.validate[CreateInvitationPayload]) match {
          case Some(JsSuccess(ItsaInvitation(invitation), _)) =>
            validateClientType(invitation) {
              validateNino(invitation.clientId) {
                // TODO WG - fix that validation should happen before AgentInvitation
                validateAgentType(invitation.agentType) {
                  checkKnownFactAndCreate(arn, invitation)
                }
              }
            }
          case Some(JsSuccess(VatInvitation(invitation), _)) =>
            validateClientType(invitation) {
              validateVrn(invitation.clientId) {
                checkKnownFactAndCreate(arn, invitation)
              }
            }
          case Some(JsSuccess(CreateInvitationPayload(List("MTD-VAT"), _, "vrn", _, _, Some(agentType)), _)) =>
            Logger(getClass).warn(s"agentType: $agentType is not supported for VAT")
            Future successful InvalidPayload
          case Some(JsSuccess(s, _)) =>
            Logger(getClass).warn(s"Unsupported service received: ${s.service.mkString("[", ",", "]")}")
            Future successful UnsupportedService
          case Some(JsError(errors)) =>
            Logger(getClass).warn(s"Invalid payload: $errors")
            Future successful InvalidPayload
          case None =>
            Logger(getClass).warn(
              s"Unsupported Content-Type, should be application/json but was ${request.contentType}"
            )
            Future successful InvalidPayload
        }
      }
    }
  }

  def getInvitationApi(givenArn: Arn, invitationId: InvitationId): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthorisedAsAgent { arn =>
        implicit val loggedInArn: Arn = arn
        forThisAgency(givenArn) {
          invitationService
            .getInvitation(arn, invitationId)
            .map {
              case pendingInv @ Some(PendingInvitation(pendingInvitation)) =>
                val id = pendingInv.get.href.toString.split("/").to(LazyList).last
                val newInvitationUrl =
                  s"${routes.AgentController.getInvitationApi(arn, InvitationId(id)).path()}"
                ga("get-authorisation-request", pendingInvitation.service.headOption)
                Ok(
                  toJson(
                    pendingInvitation
                      .copy(href = newInvitationUrl)
                  )
                    .as[JsObject]
                )
              case Some(PendingInvitation(pendingInvitation)) =>
                Logger(getClass).warn(s"Service ${pendingInvitation.service} Not Supported")
                UnsupportedService
              case respondedInv @ Some(RespondedInvitation(respondedInvitation)) =>
                val id = respondedInv.get.href.toString.split("/").to(LazyList).last
                val newInvitationUrl =
                  s"${routes.AgentController.getInvitationApi(arn, InvitationId(id)).path()}"
                ga("get-authorisation-request", respondedInvitation.service.headOption)
                Ok(
                  toJson(respondedInvitation.copy(href = newInvitationUrl))
                    .as[JsObject]
                )
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
      withAuthorisedAsAgent { arn =>
        implicit val loggedInArn: Arn = arn
        forThisAgency(givenArn) {
          invitationsConnector
            .cancelInvitation(arn, invitationId)
            .map {
              case Some(204) =>
                auditService.sendAgentInvitationCancelled(arn, invitationId.value, "Success")
                ga("cancel-authorisation-request", None)
                NoContent
              case Some(404) => InvitationNotFound
              case Some(403) => NoPermissionOnAgency
              case _ =>
                auditService.sendAgentInvitationCancelled(
                  arn,
                  invitationId.value,
                  "Fail",
                  Some(s"INVALID_INVITATION_STATUS")
                )
                Logger(getClass).warn(
                  s"Invitation Cancellation Failed: cannot transition the current status to Cancelled"
                )
                InvalidInvitationStatus
            }
            .recoverWith { case e =>
              auditService.sendAgentInvitationCancelled(
                arn,
                invitationId.value,
                "Fail",
                Some(s"Request to Cancel Invitation ${invitationId.value} failed due to: ${e.getMessage}")
              )
              Logger(getClass).warn(s"Invitation Cancellation Failed: ${e.getMessage}")
              Future.failed(e)
            }
        }
      }
    }

  def checkRelationshipApi(givenArn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      implicit val loggedInAgent: Arn = arn
      forThisAgency(givenArn) {
        val invitationResponse = request.body.asJson match {
          case Some(json) => json.as[CheckRelationshipPayload]
          case None       => CheckRelationshipPayload(List.empty, "", "", "", None)
        }
        validateAgentType(invitationResponse.agentType) {
          invitationResponse match {
            case RelationshipItsaRequest(relationship) =>
              validateNino(relationship.clientId) {
                checkKnownFactAndRelationship(arn, relationship)
              }
            case RelationshipVatRequest(relationship) =>
              validateVrn(relationship.clientId) {
                checkKnownFactAndRelationship(arn, relationship)
              }
            case s =>
              Logger(getClass).warn(s"Unsupported service received: ${s.service}")
              Future successful UnsupportedService
          }
        }
      }
    }
  }

  private def checkKnownFactAndRelationship(arn: Arn, relationshipRequest: RelationshipRequest)(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    if (checkKnownFactValid(relationshipRequest.service, relationshipRequest.knownFact)) {

      checkKnownFactMatches(relationshipRequest.service, relationshipRequest.clientId, relationshipRequest.knownFact)
        .flatMap {
          case KnownFactCheckPassed => checkRelationship(relationshipRequest, arn)
          case KnownFactCheckFailed(reason) if reason.contains("DOES_NOT_MATCH") =>
            Logger(getClass).warn(s"Postcode does not match for ${relationshipRequest.service}")
            Future successful knownFactDoesNotMatch(relationshipRequest.service)
          case KnownFactCheckFailed(reason) if reason.contains("NOT_FOUND") =>
            Logger(getClass).warn(s"Client registration not found")
            Future successful ClientRegistrationNotFound
          case KnownFactCheckFailed(reason) if reason.contains("CLIENT_INSOLVENT") =>
            Logger(getClass).warn(s"Known fact check failed: $reason")
            Future successful VatClientInsolvent
          case KnownFactCheckFailed(reason) =>
            Logger(getClass).warn(s"Known fact check failed due to: $reason")
            Future successful InternalServerError
        }
    } else {
      Logger(getClass).warn(s"Invalid Format for supplied Known Fact")
      Future successful knownFactFormatInvalid(relationshipRequest.service)
    }

  private def checkForPendingInvitationOrActiveRelationshipITSA(
    arn: Arn,
    clientId: String,
    agentType: Option[AgentType]
  )(
    successResult: => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] = {

    val allInvitationsForClient: Future[Seq[StoredInvitation]] = for {
      mainInvitationsForClient <-
        invitationsConnector.getAllInvitationsForClient(arn, clientId, MtdServie.MtdIt.enrolmentKey)
      supportingInvitationsForClient <-
        invitationsConnector.getAllInvitationsForClient(arn, clientId, MtdServie.MtdItSupp.enrolmentKey)
    } yield (mainInvitationsForClient ++ supportingInvitationsForClient)

    def checkPendingInvitationExists(whenNoPendingInvitationFound: => Future[Result]): Future[Result] =
      allInvitationsForClient
        .flatMap(
          _.find(_.status == "Pending") match {
            case Some(invitation) =>
              Future successful DuplicateAuthorisationRequest.withHeaders(LOCATION -> getLocationLink(arn, invitation))
            case None => whenNoPendingInvitationFound
          }
        )

    def checkActiveRelationshipExists(
      mtdService: MtdServie,
      whenNoActiveRelationshipFound: => Future[Result]
    ): Future[Result] = {
      val hasActiveRelationship = relationshipService.hasActiveRelationship(arn, clientId)
      hasActiveRelationship(mtdService)
        .flatMap(hasRelationship =>
          if (hasRelationship) {
            allInvitationsForClient
              .flatMap(
                _.filter(_.service == mtdService)
                  .find(_.status == "Accepted") match {
                  case Some(invitation) =>
                    Future successful AlreadyAuthorised.withHeaders(LOCATION -> getLocationLink(arn, invitation))
                  case None => whenNoActiveRelationshipFound
                }
              )
          } else whenNoActiveRelationshipFound
        )
    }

    val itsaService = agentType.map(_.service).getOrElse(AgentType.Main.service)
    checkPendingInvitationExists(
      whenNoPendingInvitationFound =
        checkActiveRelationshipExists(itsaService, whenNoActiveRelationshipFound = successResult)
    )
  }

  private def checkForPendingInvitationOrActiveRelationshipVAT(arn: Arn, clientId: String)(
    successResult: => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] = {

    val allInvitationsForClient: Future[Seq[StoredInvitation]] = invitationsConnector
      .getAllInvitationsForClient(arn, clientId, MtdServie.Vat.enrolmentKey)

    def checkPendingInvitationExists(whenNoPendingInvitationFound: => Future[Result]): Future[Result] =
      allInvitationsForClient
        .flatMap(
          _.find(_.status == "Pending") match {
            case Some(invitation) =>
              Future successful DuplicateAuthorisationRequest.withHeaders(LOCATION -> getLocationLink(arn, invitation))
            case None => whenNoPendingInvitationFound
          }
        )

    def checkActiveRelationshipExists(whenNoActiveRelationshipFound: => Future[Result]): Future[Result] = {
      val hasActiveRelationship = relationshipService.hasActiveRelationship(arn, clientId)
      hasActiveRelationship(MtdServie.Vat)
        .flatMap(hasRelationship =>
          if (hasRelationship) {
            allInvitationsForClient
              .flatMap(
                _.find(_.status == "Accepted") match {
                  case Some(invitation) =>
                    Future successful AlreadyAuthorised.withHeaders(LOCATION -> getLocationLink(arn, invitation))
                  case None => whenNoActiveRelationshipFound
                }
              )
          } else whenNoActiveRelationshipFound
        )
    }

    checkPendingInvitationExists(
      whenNoPendingInvitationFound = checkActiveRelationshipExists(whenNoActiveRelationshipFound = successResult)
    )
  }

  private def checkForPendingInvitationOrActiveRelationship(
    arn: Arn,
    clientId: String,
    service: Service,
    agentType: Option[AgentType]
  )(
    successResult: => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] =
    service match {
      case Service.Itsa =>
        checkForPendingInvitationOrActiveRelationshipITSA(arn: Arn, clientId: String, agentType)(successResult)
      case Service.Vat => checkForPendingInvitationOrActiveRelationshipVAT(arn: Arn, clientId: String)(successResult)
    }

  private def checkKnownFactAndCreate(arn: Arn, agentInvitation: AgentInvitation)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[Result] =
    if (checkKnownFactValid(agentInvitation.service, agentInvitation.knownFact)) {

      checkKnownFactMatches(
        agentInvitation.service,
        agentInvitation.clientId,
        agentInvitation.knownFact
      ).flatMap {
        case KnownFactCheckPassed =>
          checkForPendingInvitationOrActiveRelationship(
            arn,
            agentInvitation.clientId,
            agentInvitation.service,
            agentInvitation.agentType.map(AgentType(_))
          )(
            successResult = invitationService
              .createInvitation(arn, agentInvitation)
              .flatMap { invitationId =>
                val locationLink = routes.AgentController
                  .getInvitationApi(arn, InvitationId(invitationId))
                  .url
                auditService.sendAgentInvitationSubmitted(arn, invitationId, agentInvitation, "Success")
                ga("create-authorisation-request", Some(agentInvitation.service.toString))
                Future successful NoContent.withHeaders(LOCATION -> locationLink)
              }
              .recoverWith { case e =>
                Logger(getClass).warn(s"Invitation Creation Failed: ${e.getMessage}")
                auditService
                  .sendAgentInvitationSubmitted(arn, "", agentInvitation, "Fail", Some(e.getMessage))
                Future.failed(e)
              }
          )
        case KnownFactCheckFailed(reason) if reason.contains("DOES_NOT_MATCH") =>
          auditService.sendAgentInvitationSubmitted(arn, "", agentInvitation, "Fail", Some(reason))
          Future successful knownFactDoesNotMatch(agentInvitation.service)
        case KnownFactCheckFailed(reason) if reason.contains("NOT_FOUND") =>
          auditService
            .sendAgentInvitationSubmitted(arn, "", agentInvitation, "Fail", Some("CLIENT_REGISTRATION_NOT_FOUND"))
          Logger(getClass).warn(s"Client Registration Not Found")
          Future successful ClientRegistrationNotFound
        case KnownFactCheckFailed(reason) if reason.contains("CLIENT_INSOLVENT") =>
          Logger(getClass).warn(s"Invitation creation failed: $reason")
          auditService
            .sendAgentInvitationSubmitted(arn, "", agentInvitation, "Fail", Some(reason))
          Future successful VatClientInsolvent
        case KnownFactCheckFailed(reason) =>
          Logger(getClass).warn(s"invitation creation failed: $reason")
          auditService
            .sendAgentInvitationSubmitted(arn, "", agentInvitation, "Fail", Some(reason))
          Future successful InternalServerError
      }
    } else {
      Logger(getClass).warn(s"Invalid Format for supplied Known Fact")
      Future successful knownFactFormatInvalid(agentInvitation.service)
    }

  private def forThisAgency(requestedArn: Arn)(block: => Future[Result])(implicit arn: Arn) =
    if (requestedArn != arn) {
      Logger(getClass).warn(s"Requested Arn ${requestedArn.value} does not match to logged in Arn")
      Future successful NoPermissionOnAgency
    } else block

  private def checkKnownFactMatches(service: Service, clientId: String, knownFact: String)(implicit
    hc: HeaderCarrier
  ): Future[KnownFactCheckResult] =
    service match {
      case Itsa =>
        invitationsConnector.checkPostcodeForClient(Nino(clientId), knownFact)
      case Vat =>
        invitationsConnector
          .checkVatRegDateForClient(Vrn(clientId), LocalDate.parse(knownFact))
    }

  private def checkRelationship(relationshipRequest: RelationshipRequest, arn: Arn)(implicit hc: HeaderCarrier) =
    relationshipRequest.service match {
      case Itsa if relationshipRequest.agentType.getOrElse(AgentType.Main) == AgentType.Main =>
        val res = for {
          result <- relationshipsConnector.checkItsaRelationship(arn, Nino(relationshipRequest.clientId))
          _      <- ga("check-relationship", Some(relationshipRequest.service.toString))
        } yield result
        res.map {
          case true => NoContent
          case false =>
            Logger(getClass).warn(s"No ITSA main Relationship Found")
            RelationshipNotFound
        }

      // supporting
      case Itsa =>
        val res = for {
          result <- relationshipsConnector.checkItsaSuppRelationship(arn, Nino(relationshipRequest.clientId))
          _      <- ga("check-relationship", Some(MtdServie.MtdItSupp.id))
        } yield result
        res.map {
          case true => NoContent
          case false =>
            Logger(getClass).warn(s"No ITSA supporting Relationship Found")
            RelationshipNotFound
        }

      case Vat =>
        val res = for {
          result <- relationshipsConnector.checkVatRelationship(arn, Vrn(relationshipRequest.clientId))
          _      <- ga("check-relationship", Some(relationshipRequest.service.toString))
        } yield result
        res.map {
          case true => NoContent
          case false =>
            Logger(getClass).warn(s"No VAT Relationship Found")
            RelationshipNotFound
        }
    }

  def getInvitationsApi(givenArn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      implicit val loggedInArn: Arn = arn
      forThisAgency(givenArn) {
        ga("get-authorisation-requests", None)
        val previousDate =
          LocalDate.now(ZoneOffset.UTC).minusDays(getRequestsShowLastDays)
        invitationService
          .getAllInvitations(arn, previousDate)
          .map { invitations =>
            invitations
              .map {
                case pendingInv @ PendingInvitation(_) =>
                  val id = pendingInv.href.split("/").to(LazyList).last
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
                    None,
                    pendingInv.agentType
                  )

                case respondedInv @ RespondedInvitation(_) =>
                  val id = respondedInv.href.split("/").to(LazyList).last
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
                    Some(respondedInv.updated),
                    respondedInv.agentType
                  )
                case _ =>
                  // TODO Investigate implicit conversions happening for StoredInvitation(...)
                  // this should be handled by invitation unapply methods
                  throw new InternalServerException(
                    "Invalid invitation type for StoredInvitation(...) " +
                      "case should be handled or return None by unapply methods within PendingInvitation or RespondedInvitation"
                  )
              }
          }
          .map {
            case s if s.isEmpty => NoContent
            case s              => Ok(Json.toJson(s))
          }
      }
    }
  }

  private def ga(action: String, label: Option[String])(implicit hc: HeaderCarrier) =
    platformAnalyticsService.sendEvent(action, label)

  private def getLocationLink(arn: Arn, invitation: StoredInvitation): String =
    routes.AgentController.getInvitationApi(arn, InvitationId(invitation.href.split("/").last)).url
}

object AgentController {

  private val postcodeRegex =
    "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$|BFPO\\s?[0-9]{1,5}$"

  private val supportedClientTypes: Map[Service, Seq[ClientType]] =
    Map(Itsa -> Seq(personal), Vat -> Seq(personal, business))

  private val supportedAgentTypes: Seq[String] =
    Seq("main", "supporting")

  private def validateClientType(agentInvitation: AgentInvitation)(body: => Future[Result]): Future[Result] =
    if (supportedClientTypes(agentInvitation.service).contains(agentInvitation.clientType)) body
    else {
      Logger(getClass).warn(s"Unsupported Client Type")
      Future successful UnsupportedClientType
    }

  private def validateAgentType(agentType: Option[String])(body: => Future[Result]): Future[Result] =
    if (agentType.exists(supportedAgentTypes.contains) || agentType.isEmpty) body
    else {
      Logger(getClass).warn(s"Unsupported Agent Type")
      Future successful UnsupportedAgentType
    }

  private def validateNino(clientId: String)(body: => Future[Result]): Future[Result] =
    if (Nino.isValid(clientId)) body
    else if (Vrn.isValid(clientId)) {
      Logger(getClass).warn(s"Client Id does not match service")
      Future successful ClientIdDoesNotMatchService
    } else {
      Logger(getClass).warn(s"Invalid Nino provided for ITSA")
      Future successful ClientIdInvalidFormat
    }

  private def validateVrn(clientId: String)(body: => Future[Result]): Future[Result] =
    if (Vrn.isValid(clientId)) body
    else if (Nino.isValid(clientId)) {
      Logger(getClass).warn(s"Client Id does not match service")
      Future successful ClientIdDoesNotMatchService
    } else {
      Logger(getClass).warn(s"Invalid Vrn provided for VAT")
      Future successful ClientIdInvalidFormat
    }

  def validateDate(value: String): Boolean =
    if (parseDate(value)) true else false

  private val dateTimeFormat = DateTimeFormatter.ISO_LOCAL_DATE

  def parseDate(date: String): Boolean =
    try {
      dateTimeFormat.parse(date)
      true
    } catch {
      case _: Throwable => false
    }

  private def checkKnownFactValid(service: Service, knownFact: String): Boolean =
    service match {
      case Itsa =>
        knownFact.matches(postcodeRegex)
      case Vat => validateDate(knownFact)
    }

  private def knownFactDoesNotMatch(service: Service) =
    service match {
      case Itsa => PostcodeDoesNotMatch
      case Vat  => VatRegDateDoesNotMatch
    }

  private def knownFactFormatInvalid(service: Service) =
    service match {
      case Itsa => PostcodeFormatInvalid
      case Vat  => VatRegDateFormatInvalid
    }

  object ItsaInvitation {
    def unapply(arg: CreateInvitationPayload): Option[AgentInvitation] =
      arg match {
        case CreateInvitationPayload(List("MTD-IT"), "personal", "ni", _, _, _) =>
          Some(AgentInvitation(Itsa, personal, arg.clientIdType, arg.clientId, arg.knownFact, arg.agentType))
        case _ => None
      }
  }

  object VatInvitation {
    def unapply(arg: CreateInvitationPayload): Option[AgentInvitation] =
      arg match {
        case CreateInvitationPayload(List("MTD-VAT"), _, "vrn", _, _, _) =>
          Some(
            AgentInvitation(
              Vat,
              ClientType.stringToClientType(arg.clientType),
              arg.clientIdType,
              arg.clientId,
              arg.knownFact,
              None
            )
          )
        case _ => None
      }
  }

  object RelationshipItsaRequest {
    def unapply(arg: CheckRelationshipPayload): Option[RelationshipRequest] =
      arg match {
        case CheckRelationshipPayload(List("MTD-IT"), "ni", _, _, agentType) =>
          Some(RelationshipRequest(Itsa, arg.clientIdType, arg.clientId, arg.knownFact, agentType.map(AgentType(_))))
        case _ => None
      }
  }

  object RelationshipVatRequest {
    def unapply(arg: CheckRelationshipPayload): Option[RelationshipRequest] =
      arg match {
        case CheckRelationshipPayload(List("MTD-VAT"), "vrn", _, _, _) =>
          Some(RelationshipRequest(Vat, arg.clientIdType, arg.clientId, arg.knownFact, None))
        case _ => None
      }
  }

}
