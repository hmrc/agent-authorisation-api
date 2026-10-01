/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.services.v3

import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.models.v3.*
import uk.gov.hmrc.agentauthorisation.services.MongoLockService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateInvitationV3Service @Inject() (
  lockService: MongoLockService,
  acrConnector: AgentClientRelationshipsConnector
)(using ec: ExecutionContext):

  def createInvitation(arn: Arn, payload: Option[JsValue])(using
    request: RequestHeader
  ): Future[Either[ApiErrorResponse, InvitationId]] =
    CreateInvitationV3Validator.validate(payload) match
      case Left(error) => Future.successful(Left(error))
      case Right(command) =>
        val configuration = ApiServiceConfiguration.forCommand(command.service, command.clientId)
        lockService
          .acquireLock(arn.value, configuration.detailsService, command.clientId.value) {
            createWhileLocked(arn, command, configuration)
          }
          .map(_.getOrElse(Left(LockedRequest)))

  private def createWhileLocked(
    arn: Arn,
    command: ValidatedCreateInvitationV3,
    configuration: ApiServiceConfiguration
  )(using RequestHeader): Future[Either[ApiErrorResponse, InvitationId]] =
    acrConnector
      .getClientDetails(configuration.detailsService, command.clientId.value)
      .flatMap {
        case Left(error) => Future.successful(Left(error))
        case Right(details) =>
          validateClientDetails(command, details) match
            case Left(error) => Future.successful(Left(error))
            case Right(()) =>
              acrConnector.createInvitationV3(
                arn,
                AcrCreateInvitationRequest(
                  clientId = command.clientId.value,
                  suppliedClientIdType = configuration.suppliedClientIdType,
                  clientName = details.name,
                  service = configuration.createService(command, details),
                  clientType = details.clientType
                )
              )
      }

  private def validateClientDetails(
    command: ValidatedCreateInvitationV3,
    details: AcrClientDetails
  ): Either[ApiErrorResponse, Unit] =
    for
      _ <- Either.cond(details.status.isEmpty, (), ClientInsolvent)
      _ <- Either.cond(!details.hasPendingInvitation, (), DuplicateAuthorisationRequestV3)
      _ <- Either.cond(details.hasExistingRelationshipFor.isEmpty, (), AlreadyAuthorised)
      _ <- command.knownFact.fold[Either[ApiErrorResponse, Unit]](Right(())): knownFact =>
             Either.cond(
               details.knownFactType.contains(
                 AcrClientDetails.KnownFactType.fromApi(knownFact.knownFactType)
               ) && details
                 .containsKnownFact(
                   knownFact.value
                 ),
               (),
               KnownFactDoesNotMatch
             )
    yield ()
