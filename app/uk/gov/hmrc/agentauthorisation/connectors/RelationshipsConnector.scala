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

package uk.gov.hmrc.agentauthorisation.connectors

import java.net.URL
import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.connectors.Syntax.intOps
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RelationshipsConnector @Inject() (httpClient: HttpClient, metrics: Metrics, appConfig: AppConfig)
    extends HttpAPIMonitor {

  val acrUrl = s"${appConfig.acrBaseUrl}/agent-client-relationships"

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private[connectors] def checkItsaRelationshipUrl(arn: Arn, nino: Nino): URL =
    new URL(s"$acrUrl/agent/${arn.value}/service/HMRC-MTD-IT/client/NI/${nino.value}")

  private[connectors] def checkItsaSuppRelationshipUrl(arn: Arn, nino: Nino): URL =
    new URL(s"$acrUrl/agent/${arn.value}/service/HMRC-MTD-IT-SUPP/client/NI/${nino.value}")

  private[connectors] def checkVatRelationshipUrl(arn: Arn, vrn: Vrn): URL =
    new URL(s"$acrUrl/agent/${arn.value}/service/HMRC-MTD-VAT/client/VRN/${vrn.value}")

  def checkItsaRelationship(arn: Arn, nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    monitor(s"ConsumedAPI-Check-ItsaRelationship-GET") {
      val url = checkItsaRelationshipUrl(arn, nino).toString
      httpClient.GET[HttpResponse](url) map handle(url)
    }

  def checkItsaSuppRelationship(arn: Arn, nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    monitor(s"ConsumedAPI-Check-ItsaSuppRelationship-GET") {
      val url = checkItsaSuppRelationshipUrl(arn, nino).toString
      httpClient.GET[HttpResponse](url) map handle(url)
    }


  def checkVatRelationship(arn: Arn, vrn: Vrn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    monitor(s"ConsumedAPI-Check-VatRelationship-GET") {
      val url = checkVatRelationshipUrl(arn, vrn).toString
      httpClient.GET[HttpResponse](checkVatRelationshipUrl(arn, vrn).toString) map handle(url)
    }

  private val handle = (url: String) =>
    (r: HttpResponse) =>
      r match {
        case r if r.status == 404    => false
        case r if r.status.isSuccess => true
        case r =>
          throw UpstreamErrorResponse.apply(s"GET of '$url' returned ${r.status}. Response body: '${r.body}'", r.status)
      }
}
