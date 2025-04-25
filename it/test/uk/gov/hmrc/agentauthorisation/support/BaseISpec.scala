/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.support

import org.apache.pekko.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentauthorisation.stubs._

abstract class BaseISpec
    extends UnitSpec with GuiceOneServerPerSuite with WireMockSupport with AuthStubs with ACAStubs with DataStreamStubs
    with ACRStubs with TestIdentifiers {
  override implicit lazy val app: Application = appBuilder.build()

  def isEnabledItsaSupportingAgent: Boolean = true

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "auditing.enabled"                                      -> true,
        "auditing.consumer.baseUri.host"                        -> wireMockHost,
        "auditing.consumer.baseUri.port"                        -> wireMockPort,
        "microservice.services.auth.port"                       -> wireMockPort,
        "microservice.services.agent-client-authorisation.host" -> wireMockHost,
        "microservice.services.agent-client-authorisation.port" -> wireMockPort,
        "microservice.services.agent-client-relationships.port" -> wireMockPort,
        "microservice.services.agent-client-relationships.host" -> wireMockHost,
        "microservice.services.platform-analytics.host"         -> wireMockHost,
        "microservice.services.platform-analytics.port"         -> wireMockPort,
        "microservice.services.des.host"                        -> wireMockHost,
        "microservice.services.des.port"                        -> wireMockPort,
        "api.supported-versions"                                -> Seq("1.0"),
        "itsa-supporting-agent.enabled"                         -> isEnabledItsaSupportingAgent
      )
      .configure(additionalConfiguration)

  protected def additionalConfiguration = Map.empty[String, Any]

  protected implicit val materializer: Materializer = app.materializer

  def commonStubs(): Unit =
    givenAuditConnector()

  override protected def beforeEach(): Unit =
    super.beforeEach()
}
