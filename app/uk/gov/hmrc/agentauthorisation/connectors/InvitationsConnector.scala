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

package uk.gov.hmrc.agentauthorisation.connectors

import java.net.URL

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{ Inject, Named, Singleton }
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentauthorisation.models.AgentInvitation
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentauthorisation.UriPathEncoding.encodePathSegment
import uk.gov.hmrc.http.{ HeaderCarrier, HttpPost, HttpResponse }

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class InvitationsConnector @Inject() (
  @Named("agent-client-authorisation-baseUrl") baseUrl: URL,
  http: HttpPost,
  metrics: Metrics)
  extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private[connectors] def createInvitationUrl(arn: Arn): URL =
    new URL(baseUrl, s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent")

  def createInvitation(arn: Arn, agentInvitation: AgentInvitation)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[String]] =
    monitor(s"ConsumedAPI-Agent-Create-Invitation-POST") {
      http.POST[AgentInvitation, HttpResponse](createInvitationUrl(arn).toString, agentInvitation) map { r =>
        r.header("location")
      }
    }

}
