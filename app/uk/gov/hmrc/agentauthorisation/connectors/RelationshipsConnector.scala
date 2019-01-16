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

package uk.gov.hmrc.agentauthorisation.connectors

import java.net.URL

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RelationshipsConnector @Inject()(
    @Named("agent-client-relationships-baseUrl") baseUrl: URL,
    http: HttpPost with HttpGet with HttpPut,
    metrics: Metrics)
    extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private[connectors] def checkItsaRelationshipUrl(arn: Arn,
                                                   mtdItId: MtdItId): URL =
    new URL(
      baseUrl,
      s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-IT/client/MTDITID/${mtdItId.value}")

  private[connectors] def checkVatRelationshipUrl(arn: Arn, vrn: Vrn): URL =
    new URL(
      baseUrl,
      s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-VAT/client/VRN/${vrn.value}")

  def checkItsaRelationship(arn: Arn, mtdItId: MtdItId)(
      implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[Boolean] =
    monitor(s"ConsumedAPI-Check-ItsaRelationship-GET") {
      http.GET[HttpResponse](checkItsaRelationshipUrl(arn, mtdItId).toString) map (
          _ => true)
    }.recover {
      case notFound: NotFoundException => false
    }

  def checkVatRelationship(arn: Arn, vrn: Vrn)(
      implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[Boolean] =
    monitor(s"ConsumedAPI-Check-VatRelationship-GET") {
      http
        .GET[HttpResponse](checkVatRelationshipUrl(arn, vrn).toString)
        .map(_ => true)
    }.recover {
      case notFound: NotFoundException => false
    }
}
