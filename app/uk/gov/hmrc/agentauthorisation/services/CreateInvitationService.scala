/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.models.{Arn, InvitationId}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateInvitationService @Inject() (
  lockService: MongoLockService,
  acrConnector: AgentClientRelationshipsConnector
)(implicit ec: ExecutionContext) {

  def createInvitation(
    arn: Arn,
    clientAccessData: ClientAccessData
  )(implicit rh: RequestHeader): Future[Either[ApiErrorResponse, InvitationId]] =
    lockService
      .acquireLock(
        arn = arn.value,
        service = clientAccessData.service.internalServiceName,
        clientId = clientAccessData.suppliedClientId
      ) {
        acrConnector
          .createInvitation(arn, clientAccessData)
      }
      .map {
        case Some(res) => res
        case None      => Left(LockedRequest)
      }

}
