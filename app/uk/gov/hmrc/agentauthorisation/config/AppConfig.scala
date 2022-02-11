/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(servicesConfig: ServicesConfig, config: Configuration) {

  val appName = "agent-authorisation-api"

  def getConfString(key: String) =
    servicesConfig.getConfString(key, throw new RuntimeException(s"config '$key' not found"))

  def baseUrl(serviceName: String) = servicesConfig.baseUrl(serviceName)

  val authBaseUrl = baseUrl("auth")
  val acaBaseUrl = baseUrl("agent-client-authorisation")
  val acrBaseUrl = baseUrl("agent-client-relationships")

  val desBaseUrl = baseUrl("des")
  val desEnv = getConfString("des.environment")
  val desToken = getConfString("des.authorization-token")

  val showLastDays = servicesConfig.getInt("get-requests-show-last-days")

  val apiSupportedVersions = config.underlying.getStringList("api.supported-versions")

  val apiType = servicesConfig.getString("api.access.type")

  val platformAnalyticsBaseUrl = baseUrl("platform-analytics")
  val gaTrackingId: String = servicesConfig.getString("google-analytics.token")

}
