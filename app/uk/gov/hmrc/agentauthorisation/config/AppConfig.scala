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

package uk.gov.hmrc.agentauthorisation.config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util

@Singleton
class AppConfig @Inject() (servicesConfig: ServicesConfig, config: Configuration) {

  val appName = "agent-authorisation-api"

  def getConfString(key: String): String =
    servicesConfig.getConfString(key, throw new RuntimeException(s"config '$key' not found"))

  def baseUrl(serviceName: String): String = servicesConfig.baseUrl(serviceName)

  val acrBaseUrl: String = baseUrl("agent-client-relationships")
  val acrfExternalUrl: String = getConfString("agent-client-relationships-frontend.external-url")

  val apiSupportedVersions: util.List[String] = config.underlying.getStringList("api.supported-versions")

  val apiType: String = servicesConfig.getString("api.access.type")

}
