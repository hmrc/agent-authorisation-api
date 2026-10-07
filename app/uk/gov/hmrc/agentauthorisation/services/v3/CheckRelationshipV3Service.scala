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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.models.v3.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckRelationshipV3Service @Inject() (
  connector: AgentClientRelationshipsConnector
)(using executionContext: ExecutionContext):

  private final case class Target(service: String, clientIdType: String)

  def checkRelationship(
    arn: Arn,
    userId: String,
    serviceClient: ApiServiceClient
  )(using RequestHeader): Future[Either[ApiErrorResponse, CheckRelationshipV3Response]] =
    serviceClient.service match
      case ApiService.MtdIt => checkMtdIt(arn, userId, serviceClient.clientId)
      case service =>
        val configuration = ApiServiceConfiguration.forCommand(service, serviceClient.clientId)
        checkTarget(
          arn,
          userId,
          serviceClient.clientId,
          Target(configuration.detailsService, configuration.relationshipClientIdType)
        )

  private def checkMtdIt(
    arn: Arn,
    userId: String,
    clientId: ApiClientId
  )(using RequestHeader): Future[Either[ApiErrorResponse, CheckRelationshipV3Response]] =
    checkTarget(
      arn,
      userId,
      clientId,
      Target("HMRC-MTD-IT", "ni"),
      Some(RelationshipAgentType.Main)
    ).flatMap:
      case result @ Left(_) => Future.successful(result)
      case Right(response) if response.agentAuthorised => Future.successful(Right(response))
      case Right(_) =>
        checkTarget(
          arn,
          userId,
          clientId,
          Target("HMRC-MTD-IT-SUPP", "ni"),
          Some(RelationshipAgentType.Supporting)
        ).flatMap:
          case result @ Left(_) => Future.successful(result)
          case Right(response) if response.agentAuthorised => Future.successful(Right(response))
          case Right(_) => checkMtdSignup(arn, clientId)

  private def checkMtdSignup(
    arn: Arn,
    clientId: ApiClientId
  )(using RequestHeader): Future[Either[ApiErrorResponse, CheckRelationshipV3Response]] =
    connector
      .relationshipExistsV3(arn, "IR-SA", "ni", clientId.value, None)
      .map:
        case Left(error) => Left(error)
        case Right(true) =>
          Right(CheckRelationshipV3Response.authorised(userAssigned = true, Some(RelationshipAgentType.MtdSignup)))
        case Right(false) => Right(CheckRelationshipV3Response.NotAuthorised)

  private def checkTarget(
    arn: Arn,
    userId: String,
    clientId: ApiClientId,
    target: Target,
    agentType: Option[RelationshipAgentType] = None
  )(using RequestHeader): Future[Either[ApiErrorResponse, CheckRelationshipV3Response]] =
    connector
      .relationshipExistsV3(arn, target.service, target.clientIdType, clientId.value, Some(userId))
      .flatMap:
        case Left(error) => Future.successful(Left(error))
        case Right(true) =>
          Future.successful(Right(CheckRelationshipV3Response.authorised(userAssigned = true, agentType)))
        case Right(false) =>
          connector
            .relationshipExistsV3(arn, target.service, target.clientIdType, clientId.value, None)
            .map:
              case Left(error) => Left(error)
              case Right(true) =>
                Right(CheckRelationshipV3Response.authorised(userAssigned = false, agentType))
              case Right(false) => Right(CheckRelationshipV3Response.NotAuthorised)
