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

import com.google.inject.ImplementedBy
import org.apache.pekko.Done
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models.AnalyticsRequest
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[PlatformAnalyticsConnectorImpl])
trait PlatformAnalyticsConnector {
  def sendEvent(request: AnalyticsRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Done]
}

@Singleton
class PlatformAnalyticsConnectorImpl @Inject() (appConfig: AppConfig, http: HttpClientV2)
    extends PlatformAnalyticsConnector with HttpErrorFunctions {

  def sendEvent(request: AnalyticsRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Done] = {
    val requestUrl = url"${appConfig.platformAnalyticsBaseUrl}/platform-analytics/event"
    http
      .post(requestUrl)
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case status if is2xx(status) => Done
          case other =>
            Logger(getClass).warn(s"Couldn't send analytics event, response status: $other")
            Done
        }
      }
      .recover { case NonFatal(ex) =>
        Logger(getClass).warn(s"Couldn't send analytics event, error: $ex")
        Done
      }
  }
}
