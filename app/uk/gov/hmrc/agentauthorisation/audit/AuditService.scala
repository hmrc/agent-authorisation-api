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

package uk.gov.hmrc.agentauthorisation.audit

import javax.inject.{ Inject, Singleton }
import play.api.mvc.Request
import uk.gov.hmrc.agentauthorisation.audit.AgentAuthorisationEvent.AgentAuthorisationEvent
import uk.gov.hmrc.agentauthorisation.models.{ AgentInvitation, Invitation }
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.Try

object AgentAuthorisationEvent extends Enumeration {
  val AgentAuthorisationCreatedViaApi, AgentAuthorisedCancelledViaApi, AgentCheckRelationshipStatusApi, AgentGetInvitationApi = Value
  type AgentAuthorisationEvent = Value
}

@Singleton
class AuditService @Inject() (val auditConnector: AuditConnector) {

  private[audit] def auditEvent(
    event: AgentAuthorisationEvent,
    transactionName: String,
    details: Seq[(String, Any)] = Seq.empty)(implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] =
    send(createEvent(event, transactionName, details: _*))

  def sendAgentInvitationSubmitted(
    arn: Arn,
    invitationId: String,
    agentInvitation: AgentInvitation,
    result: String,
    failure: Option[String] = None)(implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] =
    auditEvent(
      AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi,
      "Agent created invitation through third party software",
      Seq(
        "factCheck" -> result,
        "invitationId" -> invitationId,
        "agentReferenceNumber" -> arn.value,
        "clientIdType" -> agentInvitation.clientIdType,
        "clientId" -> agentInvitation.clientId,
        "service" -> agentInvitation.service).filter(_._2.nonEmpty) ++ failure.map(e => Seq("failureDescription" -> e)).getOrElse(Seq.empty))

  def sendAgentGetInvitation(
    arn: Arn,
    invitationId: String,
    result: String,
    invitation: Option[Invitation] = None,
    failure: Option[String] = None)(implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] =
    auditEvent(
      AgentAuthorisationEvent.AgentGetInvitationApi,
      "Agent retrieved invitation through third party software",
      Seq(
        "result" -> result,
        "invitationId" -> invitationId,
        "agentReferenceNumber" -> arn.value).filter(_._2.nonEmpty) ++
        invitation.map(i => Seq("service" -> i.service, "status" -> i.status)).getOrElse(Seq.empty) ++
        failure.map(e => Seq("failureDescription" -> e)).getOrElse(Seq.empty))

  def sendAgentInvitationCancelled(
    arn: Arn,
    invitationId: String,
    result: String,
    failure: Option[String] = None)(implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] =
    auditEvent(
      AgentAuthorisationEvent.AgentAuthorisedCancelledViaApi,
      "Agent cancelled invitation through third party software",
      Seq(
        "result" -> result,
        "invitationId" -> invitationId,
        "agentReferenceNumber" -> arn.value).filter(_._2.nonEmpty) ++ failure.map(e => Seq("failureDescription" -> e)).getOrElse(Seq.empty))

  def sendAgentCheckRelationshipStatus(
    arn: Arn,
    agentInvitation: AgentInvitation,
    result: String,
    failure: Option[String] = None)(implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] =
    auditEvent(
      AgentAuthorisationEvent.AgentCheckRelationshipStatusApi,
      "Agent checked status of relationship through third party software",
      Seq(
        "result" -> result,
        "agentReferenceNumber" -> arn.value,
        "service" -> agentInvitation.service,
        "clientId" -> agentInvitation.clientId,
        "clientIdType" -> agentInvitation.clientIdType).filter(_._2.nonEmpty) ++ failure.map(e => Seq("failureDescription" -> e)).getOrElse(Seq.empty))

  private def createEvent(event: AgentAuthorisationEvent, transactionName: String, details: (String, Any)*)(
    implicit
    hc: HeaderCarrier,
    request: Request[Any]): DataEvent = {

    val detail = hc.toAuditDetails(details.map(pair => pair._1 -> pair._2.toString): _*)
    val tags = hc.toAuditTags(transactionName, request.path)
    DataEvent(auditSource = "agent-authorisation-api", auditType = event.toString, tags = tags, detail = detail)
  }

  private def send(events: DataEvent*)(implicit hc: HeaderCarrier): Future[Unit] =
    Future {
      events.foreach { event =>
        Try(auditConnector.sendEvent(event))
      }
    }
}