/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.services.v3

import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.models.ApiService.MtdIt
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.support.BaseSpec
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.concurrent.{ExecutionContext, Future}

class DeauthoriseRelationshipV3ServiceSpec extends BaseSpec with MockFactory:

  given ExecutionContext = scala.concurrent.ExecutionContext.global

  private val config = Configuration.apply(
    "api.supported-versions"                                                 -> List("1.0"),
    "api.access.type"                                                        -> "PRIVATE",
    "microservice.services.agent-client-relationships.host"                  -> "localhost",
    "microservice.services.agent-client-relationships.port"                  -> 9434,
    "microservice.services.agent-client-relationships-frontend.external-url" -> "http://localhost"
  )

  private val appConfig = AppConfig(ServicesConfig(config), config)

  private class StubAcrConnector(response: Either[ApiErrorResponse, Unit])
      extends AgentClientRelationshipsConnector(
        httpClient = mock[HttpClientV2],
        metrics = mock[Metrics],
        appConfig = appConfig
      ):

    var capturedCall: Option[(Arn, String, String)] = None
    var capturedRequestHeader: Option[RequestHeader] = None

    override def removeAuthorisation(arn: Arn, clientId: String, service: String)(using
      RequestHeader
    ): Future[Either[ApiErrorResponse, Unit]] =
      capturedCall = Some((arn, clientId, service))
      capturedRequestHeader = Some(summon[RequestHeader])
      Future.successful(response)

  "DeauthoriseRelationshipV3Service" should:

    "delegate removeAuthorisation to the ACR connector with the same parameters and RequestHeader" in:
      val acrConnector = new StubAcrConnector(Right(()))
      val service = new DeauthoriseRelationshipV3Service(acrConnector)

      val fakeRequest = FakeRequest("DELETE", "/agent/relationships")
        .withHeaders("Authorization" -> "Bearer token")
      given RequestHeader = testRequest(fakeRequest)

      val result = service.removeAuthorisation(arn, MtdIt, ApiClientId.from("123456789").get).futureValue

      result.shouldBe(Right(()))
      acrConnector.capturedCall.value.shouldBe((arn, "123456789", "HMRC-MTD-IT"))
      acrConnector.capturedRequestHeader.value.method.shouldBe("DELETE")
      acrConnector.capturedRequestHeader.value.path.shouldBe("/agent/relationships")
      acrConnector.capturedRequestHeader.value.headers.get("Authorization").shouldBe(Some("Bearer token"))

    "propagate connector error responses unchanged" in:
      val acrConnector = new StubAcrConnector(Left(NoRelationship))
      val service = new DeauthoriseRelationshipV3Service(acrConnector)

      given RequestHeader = testRequest(FakeRequest())

      val result = service.removeAuthorisation(arn, MtdIt, ApiClientId.from("123456789").get).futureValue

      result.shouldBe(Left(NoRelationship))
