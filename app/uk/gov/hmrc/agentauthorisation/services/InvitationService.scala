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

package uk.gov.hmrc.agentauthorisation.services

import javax.inject.{ Inject, Singleton }
import org.joda.time.LocalDate
import uk.gov.hmrc.agentauthorisation.connectors.InvitationsConnector

import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{ Arn, InvitationId, Vrn }
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class InvitationService @Inject() (invitationsConnector: InvitationsConnector) {

  def createInvitationService(arn: Arn, agentInvitation: AgentInvitation)(
    implicit
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext): Future[String] =
    invitationsConnector
      .createInvitation(arn, agentInvitation)
      .map(_.getOrElse(throw new Exception("Invitation location expected but missing.")))

  def checkPostcodeMatches(nino: Nino, postcode: String)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[Boolean]] =
    invitationsConnector.checkPostcodeForClient(nino, postcode)

  def checkVatRegDateMatches(vrn: Vrn, vatRegDate: LocalDate)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[Boolean]] =
    invitationsConnector.checkVatRegDateForClient(vrn, vatRegDate)

  def getInvitationService(arn: Arn, invitationId: InvitationId)(
    implicit
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext): Future[Option[StoredInvitation]] =
    invitationsConnector.getInvitation(arn, invitationId)

  def cancelInvitationService(arn: Arn, invitationId: InvitationId)(
    implicit
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext): Future[Option[Int]] =
    invitationsConnector.cancelInvitation(arn, invitationId)

}
