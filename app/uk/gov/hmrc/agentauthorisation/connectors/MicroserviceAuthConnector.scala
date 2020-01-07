/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.inject.{Inject, Named, Singleton}
import play.api.Configuration
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.otac.PlayOtacAuthConnector
import uk.gov.hmrc.http.{HttpGet, HttpPost}
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}

@Singleton
class MicroserviceAuthConnector @Inject()(
  @Named("auth-baseUrl") baseUrl: URL,
  config: Configuration,
  val _actorSystem: ActorSystem)
    extends PlayAuthConnector with PlayOtacAuthConnector {

  override val serviceUrl = baseUrl.toString

  override def http = new HttpPost with HttpGet with WSPost with WSGet {
    override val hooks = NoneRequired

    override protected def actorSystem: ActorSystem = _actorSystem

    override protected def configuration: Option[Config] = Some(config.underlying)
  }
}
