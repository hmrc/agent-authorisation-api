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

import org.apache.pekko.Done
import com.google.inject.ImplementedBy
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models.AnalyticsRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse}
import play.api.Logger
import uk.gov.hmrc.http.HttpClient
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import uk.gov.hmrc.http.HttpReads.Implicits._

@ImplementedBy(classOf[PlatformAnalyticsConnectorImpl])
trait PlatformAnalyticsConnector {
  def sendEvent(request: AnalyticsRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Done]
}

@Singleton
class PlatformAnalyticsConnectorImpl @Inject() (appConfig: AppConfig, http: HttpClient)
    extends PlatformAnalyticsConnector with HttpErrorFunctions {

  val serviceUrl: String = s"${appConfig.platformAnalyticsBaseUrl}/platform-analytics/event"

  def sendEvent(request: AnalyticsRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Done] =
    http
      .POST[AnalyticsRequest, HttpResponse](serviceUrl, request)
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
