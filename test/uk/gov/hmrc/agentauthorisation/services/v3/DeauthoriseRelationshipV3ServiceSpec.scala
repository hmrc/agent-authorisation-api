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
import org.scalatest.Inspectors
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Configuration
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.models.ApiService.{MtdIt, MtdVat}
import uk.gov.hmrc.agentauthorisation.support.BaseSpec
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class DeauthoriseRelationshipV3ServiceSpec extends BaseSpec with MockFactory with TableDrivenPropertyChecks:

  given ExecutionContext = scala.concurrent.ExecutionContext.global

  private val config = Configuration.apply(
    "api.supported-versions"                                                 -> List("1.0"),
    "api.access.type"                                                        -> "PRIVATE",
    "microservice.services.agent-client-relationships.host"                  -> "localhost",
    "microservice.services.agent-client-relationships.port"                  -> 9434,
    "microservice.services.agent-client-relationships-frontend.external-url" -> "http://localhost"
  )

  private val appConfig = AppConfig(ServicesConfig(config), config)

  private class StubAcrConnector(serviceToResponse: Map[String, Either[ApiErrorResponse, Unit]])
      extends AgentClientRelationshipsConnector(
        httpClient = mock[HttpClientV2],
        metrics = mock[Metrics],
        appConfig = appConfig
      ):

    val capturedCalls: mutable.ListBuffer[(Arn, String, String)] = mutable.ListBuffer.empty
    val capturedRequestHeaders: mutable.ListBuffer[RequestHeader] = mutable.ListBuffer.empty

    override def removeAuthorisation(arn: Arn, clientId: String, service: String)(using
      RequestHeader
    ): Future[Either[ApiErrorResponse, Unit]] =
      capturedCalls += ((arn, clientId, service))
      capturedRequestHeaders += summon[RequestHeader]
      Future.successful(serviceToResponse.getOrElse(service, Right(())))

  "DeauthoriseRelationshipV3Service" when:

    "delegate removeAuthorisation to the ACR connector with the same parameters and RequestHeader" in:
      val acrConnector = new StubAcrConnector(Map("HMRC-MTD-IT" -> Right(())))
      val service = new DeauthoriseRelationshipV3Service(acrConnector)

      given RequestHeader = FakeRequest("DELETE", "/agent/relationships")
        .withHeaders("Authorization" -> "Bearer token")

      val result = service.removeAuthorisation(arn, MtdIt, ApiClientId.from("AB123456A").get).futureValue

      result.shouldBe(Right(()))

      Inspectors.forAll(acrConnector.capturedRequestHeaders): requestHeader =>
        requestHeader.method.shouldBe("DELETE")
        requestHeader.path.shouldBe("/agent/relationships")
        requestHeader.headers.get("Authorization").shouldBe(Some("Bearer token"))

    "handle 'no relationship' responses as acceptable responses" in:
      val acrConnector = new StubAcrConnector(Map("HMRC-MTD-IT" -> Left(NoRelationship)))
      val service = new DeauthoriseRelationshipV3Service(acrConnector)

      given RequestHeader = FakeRequest("DELETE", "/agent/relationships")
        .withHeaders("Authorization" -> "Bearer token")

      val result = service.removeAuthorisation(arn, MtdIt, ApiClientId.from("AB123456A").get).futureValue

      result.shouldBe(Right(()))

  "remove all MTD-IT relationship types (main and supporting)" in:
    val acrConnector = new StubAcrConnector(Map("HMRC-MTD-IT" -> Right(()), "HMRC-MTD-IT-SUPP" -> Right(())))
    val service = new DeauthoriseRelationshipV3Service(acrConnector)

    val fakeRequest = FakeRequest("DELETE", "/agent/relationships")
      .withHeaders("Authorization" -> "Bearer token")

    given RequestHeader = testRequest(fakeRequest)

    val result = service.removeAuthorisation(arn, MtdIt, ApiClientId.from("AB123456A").get).futureValue

    result.shouldBe(Right(()))
    acrConnector.capturedCalls should contain only (
      (arn, "AB123456A", "HMRC-MTD-IT"),
      (arn, "AB123456A", "HMRC-MTD-IT-SUPP")
    )

    Inspectors.forAll(acrConnector.capturedRequestHeaders): requestHeader =>
      requestHeader.method.shouldBe("DELETE")
      requestHeader.path.shouldBe("/agent/relationships")
      requestHeader.headers.get("Authorization").shouldBe(Some("Bearer token"))

  "reject invalid pairings of service and client" in:
    val acrConnector = new StubAcrConnector(Map("HMRC-MTD-VAT" -> Right(())))
    val service = new DeauthoriseRelationshipV3Service(acrConnector)

    given RequestHeader = testRequest(FakeRequest())

    val result = service.removeAuthorisation(arn, MtdVat, ApiClientId.from("XACBC0123456789").get).futureValue

    result.shouldBe(Left(ClientIdNotCompatibleWithService))

  "handling errors in associated relationship removals" should:
    val FailureScenarios = Table(
      ("Main Response", "Associated Response", "Expected Result"),
      (Right(()), Right(()), Right(())),
      (Right(()), Left(NoRelationship), Right(())),
      (Left(NoRelationship), Right(()), Right(())),
      (Left(NoRelationship), Left(NoRelationship), Right(())),
      (Right(()), Left(StandardInternalServerError), Left(StandardInternalServerError)),
      (Left(NoRelationship), Left(StandardInternalServerError), Left(StandardInternalServerError)),
      (Left(StandardInternalServerError), Right(()), Left(StandardInternalServerError)),
      (Left(StandardInternalServerError), Left(NoRelationship), Left(StandardInternalServerError)),
      // Errors when deleting the main relationship take precedence over errors when deleting the associated relationship
      (Left(StandardInternalServerError), Left(ClientRegistrationNotFound), Left(StandardInternalServerError))
    )

    forAll(FailureScenarios): (mainResponse, associatedResponse, expectedResult) =>
      s"return $expectedResult when main returns $mainResponse and associated returns $associatedResponse" in:
        val acrConnector = StubAcrConnector(Map("HMRC-MTD-IT" -> mainResponse, "HMRC-MTD-IT-SUPP" -> associatedResponse))
        val service = new DeauthoriseRelationshipV3Service(acrConnector)

        given RequestHeader = FakeRequest()

        val result = service.removeAuthorisation(arn, MtdIt, ApiClientId.from("AB123456A").get).futureValue

        result.shouldBe(expectedResult)
