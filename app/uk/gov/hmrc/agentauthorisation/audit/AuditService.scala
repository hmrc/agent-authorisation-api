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

package uk.gov.hmrc.agentauthorisation.audit

import play.api.mvc.Request
import uk.gov.hmrc.agentauthorisation.audit.AgentAuthorisationEvent.AgentAuthorisationEvent
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object AgentAuthorisationEvent extends Enumeration {
  val agentAuthorisationCreatedViaApi, agentAuthorisedCancelledViaApi = Value
  type AgentAuthorisationEvent = Value
}

@Singleton
class AuditService @Inject() (val auditConnector: AuditConnector) {

  private[audit] def auditEvent(
    event: AgentAuthorisationEvent,
    transactionName: String,
    details: Seq[(String, Any)] = Seq.empty
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] =
    send(createEvent(event, transactionName, details: _*))

  def sendAgentInvitationCancelled(arn: Arn, invitationId: String, result: String, failure: Option[String] = None)(
    implicit
    hc: HeaderCarrier,
    request: Request[Any],
    ec: ExecutionContext
  ): Future[Unit] =
    auditEvent(
      AgentAuthorisationEvent.agentAuthorisedCancelledViaApi,
      "agent-cancelled-invitation-via-api",
      Seq("result" -> result, "invitationId" -> invitationId, "agentReferenceNumber" -> arn.value)
        .filter(_._2.nonEmpty) ++ failure
        .map(e => Seq("failureDescription" -> e))
        .getOrElse(Seq.empty)
    )

  private def createEvent(event: AgentAuthorisationEvent, transactionName: String, details: (String, Any)*)(implicit
    hc: HeaderCarrier,
    request: Request[Any]
  ): DataEvent = {

    val detail =
      hc.toAuditDetails(details.map(pair => pair._1 -> pair._2.toString): _*)
    val tags = hc.toAuditTags(transactionName, request.path)
    DataEvent(auditSource = "agent-authorisation-api", auditType = event.toString, tags = tags, detail = detail)
  }

  private def send(events: DataEvent*)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future {
      events.foreach { event =>
        Try(auditConnector.sendEvent(event))
      }
    }
}
