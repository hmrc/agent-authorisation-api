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

package uk.gov.hmrc.agentauthorisation.services

import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class InvitationService @Inject() (
  agentClientRelationshipsConnector: AgentClientRelationshipsConnector
) {

  def getInvitation(arn: Arn, invitationId: InvitationId)(implicit
    headerCarrier: HeaderCarrier
  ): Future[Either[ApiErrorResponse, SingleInvitationDetails]] =
    agentClientRelationshipsConnector.getInvitation(arn, invitationId)

  def getAllInvitations(arn: Arn)(implicit
    hc: HeaderCarrier
  ): Future[Either[ApiErrorResponse, Option[AllInvitationDetails]]] =
    agentClientRelationshipsConnector.getAllInvitations(arn)

}
