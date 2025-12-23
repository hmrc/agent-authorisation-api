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

import com.google.inject.AbstractModule
import org.apache.pekko.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentauthorisation.services.MongoLockService
import uk.gov.hmrc.agentauthorisation.stubs._
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

import scala.concurrent.ExecutionContext.Implicits.global

abstract class BaseISpec
    extends UnitSpec with GuiceOneServerPerSuite with WireMockSupport with AuthStubs with MongoApp with DataStreamStubs
    with ACRStubs with TestIdentifiers {

  override implicit lazy val app: Application = appBuilder.build()

  def isEnabledItsaSupportingAgent: Boolean = true

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.enabled"                                       -> false,
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
        "api.supported-versions"                                -> Seq("1.0", "2.0"),
        "itsa-supporting-agent.enabled"                         -> isEnabledItsaSupportingAgent,
        "mongodb.uri"                                           -> mongoUri
      )
      .configure(additionalConfiguration)

  protected def additionalConfiguration = Map.empty[String, Any]

  protected implicit val materializer: Materializer = app.materializer

  lazy val mongoLockService: MongoLockService = new MongoLockService(mongoLockRepository)
  def mongoLockRepository = new MongoLockRepository(mongoComponent, new CurrentTimestampSupport)

  lazy val moduleWithOverrides = new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[MongoComponent]).toInstance(mongoComponent)
      bind(classOf[MongoLockService]).toInstance(mongoLockService)
    }
  }

  def commonStubs(): Unit =
    givenAuditConnector()

  override protected def beforeEach(): Unit =
    super.beforeEach()
}
