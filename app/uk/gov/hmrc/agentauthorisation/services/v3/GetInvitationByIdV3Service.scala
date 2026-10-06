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
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.models.v3.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetInvitationByIdV3Service @Inject() (
  acrConnector: AgentClientRelationshipsConnector,
  appConfig: AppConfig
)(using ec: ExecutionContext):

  def getInvitation(arn: Arn, invitationId: InvitationId)(using
    request: RequestHeader
  ): Future[Either[ApiErrorResponse, GetInvitationByIdV3Response]] =
    acrConnector.getInvitationV3(arn, invitationId).map(_.flatMap(toResponse))

  private def toResponse(invitationInfo: AcrInvitationInfoV3): Either[ApiErrorResponse, GetInvitationByIdV3Response] =
    val invitation = invitationInfo.authorisationRequest

    ApiServiceConfiguration
      .forAcrInvitationService(invitation.service)
      .toRight(StandardInternalServerError)
      .map: configuration =>
        val isPending = invitation.status == ApiInvitationStatus.Pending
        val clientActionUrl = Option.when(isPending):
          s"${appConfig.acrfExternalUrl}/agent-client-relationships/appoint-someone-to-deal-with-HMRC-for-you" +
            s"/${invitationInfo.agentLink.uid}/${invitationInfo.agentLink.normalizedAgentName}" +
            s"/${configuration.clientActionUrlPart}"

        GetInvitationByIdV3Response(
          invitationId = invitation.invitationId,
          service = configuration.service,
          status = invitation.status,
          created = invitation.created,
          updated = Option.when(!isPending)(invitation.lastUpdated),
          expiresOn = Option.when(isPending)(invitation.expiryDate),
          clientActionUrl = clientActionUrl,
          agentType = configuration.agentType
        )
