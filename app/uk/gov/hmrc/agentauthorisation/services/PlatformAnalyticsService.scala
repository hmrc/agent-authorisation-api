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

import org.apache.pekko.Done
import com.google.inject.ImplementedBy
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.connectors.PlatformAnalyticsConnector
import uk.gov.hmrc.agentauthorisation.models.{AnalyticsRequest, Event}
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.hashing.{MurmurHash3 => MH3}

@ImplementedBy(classOf[PlatformAnalyticsServiceImpl])
trait PlatformAnalyticsService {
  def sendEvent(action: String, label: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Done]
}

@Singleton
class PlatformAnalyticsServiceImpl @Inject() (connector: PlatformAnalyticsConnector, appConfig: AppConfig)
    extends PlatformAnalyticsService {
  private val trackingId = appConfig.gaTrackingId

  def sendEvent(action: String, label: Option[String])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Done] = {
    val maybeGAClientId: Option[String] = if (hc.sessionId.isDefined) None else Some(makeGAClientId)
    connector.sendEvent(
      AnalyticsRequest(
        maybeGAClientId,
        Some(trackingId),
        List(Event("agent-authorisation-api", action, label.getOrElse(""), Seq.empty))
      )
    )
  }

  def makeGAClientId: String = {
    val uuid = UUID.randomUUID().toString
    MH3.stringHash(uuid, MH3.stringSeed).abs.toString match {
      case uuidHash =>
        "GA1.1." + (uuidHash + "000000000")
          .substring(0, 9) + "." + ("0000000000" + uuidHash).substring(uuidHash.length, 10 + uuidHash.length).reverse
    }
  }

}
