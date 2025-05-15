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

import play.api.http.Status.{FORBIDDEN, LOCKED, NOT_FOUND, NO_CONTENT}
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.util.HttpAPIMonitor
import uk.gov.hmrc.agentmtdidentifiers.model.Vrn
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InvitationsConnector @Inject() (httpClient: HttpClientV2, val metrics: Metrics, appConfig: AppConfig)(implicit
  val ec: ExecutionContext
) extends HttpAPIMonitor {

  val acaUrl = s"${appConfig.acaBaseUrl}/agent-client-authorisation"

  import uk.gov.hmrc.http.HttpReads.Implicits._

  def checkPostcodeForClient(nino: Nino, postcode: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[KnownFactCheckResult] =
    monitor(s"ConsumedAPI-CheckPostcode-GET") {
      val requestUrl = url"$acaUrl/known-facts/individuals/nino/${nino.value}/sa/postcode/$postcode"
      httpClient
        .get(requestUrl)
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case NO_CONTENT => KnownFactCheckPassed
            case FORBIDDEN =>
              KnownFactCheckFailed((response.json \ "code").as[String])
            case other => KnownFactCheckFailed(s"Failed due to status $other")
          }
        }
    }

  def checkVatRegDateForClient(vrn: Vrn, registrationDateKnownFact: LocalDate)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[KnownFactCheckResult] =
    monitor(s"ConsumedAPI-CheckVatRegDate-GET") {
      val requestUrl =
        url"$acaUrl/known-facts/organisations/vat/${vrn.value}/registration-date/${registrationDateKnownFact.toString}"
      httpClient
        .get(requestUrl)
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case NO_CONTENT => KnownFactCheckPassed
            case FORBIDDEN  => KnownFactCheckFailed((response.json \ "code").as[String])
            case NOT_FOUND  => KnownFactCheckFailed("VAT_RECORD_NOT_FOUND")
            case LOCKED     => KnownFactCheckFailed("MIGRATION_IN_PROGRESS")
            case other      => KnownFactCheckFailed(s"Failed due to status $other")
          }
        }
    }

}
