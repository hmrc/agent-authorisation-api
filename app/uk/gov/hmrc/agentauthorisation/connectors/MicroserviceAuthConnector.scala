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

import javax.inject.{ Inject, Named, Singleton }
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.otac.PlayOtacAuthConnector
import uk.gov.hmrc.http.{ HttpGet, HttpPost }
import uk.gov.hmrc.play.http.ws.{ WSGet, WSPost }

@Singleton
class MicroserviceAuthConnector @Inject() (@Named("auth-baseUrl") baseUrl: URL) extends PlayAuthConnector with PlayOtacAuthConnector {

  override val serviceUrl = baseUrl.toString

  override def http = new HttpPost with HttpGet with WSPost with WSGet {
    override val hooks = NoneRequired
  }
}
