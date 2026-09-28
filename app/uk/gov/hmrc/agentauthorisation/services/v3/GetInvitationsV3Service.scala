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
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.controllers.routes
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.models.v3.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetInvitationsV3Service @Inject() (
  acrConnector: AgentClientRelationshipsConnector,
  appConfig: AppConfig
)(using ec: ExecutionContext):

  def getInvitations(arn: Arn)(using
    request: RequestHeader
  ): Future[Either[ApiErrorResponse, Seq[GetInvitationV3Response]]] =
    acrConnector.getInvitationsV3(arn).flatMap:
      case Left(error) => Future.successful(Left(error))
      case Right(invitations) if invitations.exists(_.status == ApiInvitationStatus.Pending) =>
        acrConnector.getAgentLinkV3().map(_.flatMap(link => mapInvitations(arn, invitations, Some(link))))
      case Right(invitations) =>
        Future.successful(mapInvitations(arn, invitations, None))

  private def mapInvitations(
    arn: Arn,
    invitations: Seq[AcrInvitationV3],
    agentLink: Option[AcrAgentLinkV3]
  ): Either[ApiErrorResponse, Seq[GetInvitationV3Response]] =
    invitations.foldLeft[Either[ApiErrorResponse, Vector[GetInvitationV3Response]]](Right(Vector.empty)):
      case (result, invitation) =>
        for
          responses <- result
          configuration <- ApiServiceConfiguration
                             .forAcrInvitationService(invitation.service)
                             .toRight(StandardInternalServerError)
        yield responses :+ toResponse(arn, invitation, configuration, agentLink)

  private def toResponse(
    arn: Arn,
    invitation: AcrInvitationV3,
    configuration: ApiInvitationServiceConfiguration,
    agentLink: Option[AcrAgentLinkV3]
  ): GetInvitationV3Response =
    val isPending = invitation.status == ApiInvitationStatus.Pending
    val clientActionUrl = Option.when(isPending)(agentLink).flatten.map: link =>
      s"${appConfig.acrfExternalUrl}/agent-client-relationships/appoint-someone-to-deal-with-HMRC-for-you" +
        s"/${link.uid}/${link.normalizedAgentName}/${configuration.clientActionUrlPart}"

    GetInvitationV3Response(
      href = routes.GetInvitationsController
        .getInvitationApi(arn, InvitationId(invitation.invitationId))
        .path(),
      service = configuration.service,
      status = invitation.status,
      created = invitation.created,
      updated = Option.when(!isPending)(invitation.lastUpdated),
      expiresOn = Option.when(isPending)(invitation.expiryDate),
      clientActionUrl = clientActionUrl,
      agentType = configuration.agentType
    )
