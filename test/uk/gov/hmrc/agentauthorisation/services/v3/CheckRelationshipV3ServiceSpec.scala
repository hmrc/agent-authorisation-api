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
import org.scalatest.EitherValues.convertEitherToValuable
import play.api.Configuration
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.models.v3.{CheckRelationshipV3Response, RelationshipAgentType}
import uk.gov.hmrc.agentauthorisation.support.BaseSpec
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class CheckRelationshipV3ServiceSpec extends BaseSpec with MockFactory:

  private given ExecutionContext = ExecutionContext.global
  private given RequestHeader = testRequest(FakeRequest())
  private val userId = "agent-user-id"

  private val configuration = Configuration(
    "api.supported-versions"                                -> List("1.0"),
    "api.access.type"                                       -> "PRIVATE",
    "microservice.services.agent-client-relationships.host" -> "localhost",
    "microservice.services.agent-client-relationships.port" -> 9434,
    "microservice.services.agent-client-relationships-frontend.external-url" -> "http://localhost"
  )
  private val appConfig = AppConfig(ServicesConfig(configuration), configuration)

  private final case class Call(service: String, clientIdType: String, clientId: String, userId: Option[String])

  private class StubConnector(responses: Either[ApiErrorResponse, Boolean]*)
      extends AgentClientRelationshipsConnector(mock[HttpClientV2], mock[Metrics], appConfig):
    val calls: mutable.ArrayBuffer[Call] = mutable.ArrayBuffer.empty
    private val remaining = mutable.Queue.from(responses)

    override def relationshipExistsV3(
      arn: Arn,
      service: String,
      clientIdType: String,
      clientId: String,
      userId: Option[String]
    )(using RequestHeader): Future[Either[ApiErrorResponse, Boolean]] =
      calls += Call(service, clientIdType, clientId, userId)
      Future.successful(remaining.dequeue())

  private def serviceClient(service: String, clientId: String): ApiServiceClient =
    ApiServiceClient.validate(service, clientId).value

  "CheckRelationshipV3Service" should:
    "use the correct ACR service and identifier for every non-MTD-IT service" in:
      val scenarios = Seq(
        ("MTD-VAT", "101747696", "HMRC-MTD-VAT", "VRN"),
        ("TRUSTS", "1234567890", "HMRC-TERS-ORG", "SAUTR"),
        ("TRUSTS", "XXTRUST12345678", "HMRC-TERSNT-ORG", "URN"),
        ("IRV", "AB123456A", "PERSONAL-INCOME-RECORD", "ni"),
        ("CGT-PD", "XACGTP123456789", "HMRC-CGT-PD", "CGTPDRef"),
        ("PPT", "XAPPT0001234567", "HMRC-PPT-ORG", "EtmpRegistrationNumber"),
        ("CBC", "XACBC1234567890", "HMRC-CBC-ORG", "cbcId"),
        ("PILLAR2", "XAPLR1234567890", "HMRC-PILLAR2-ORG", "PLRID")
      )

      scenarios.foreach: (publicService, clientId, acrService, clientIdType) =>
        val connector = StubConnector(Right(true))
        val result = CheckRelationshipV3Service(connector)
          .checkRelationship(arn, userId, serviceClient(publicService, clientId))
          .futureValue

        result shouldBe Right(CheckRelationshipV3Response.authorised(userAssigned = true))
        connector.calls.toSeq shouldBe Seq(Call(acrService, clientIdType, clientId, Some(userId)))

    "distinguish an unassigned user from an unrelated agency" in:
      val client = serviceClient("MTD-VAT", "101747696")

      val unassignedConnector = StubConnector(Right(false), Right(true))
      CheckRelationshipV3Service(unassignedConnector)
        .checkRelationship(arn, userId, client)
        .futureValue shouldBe Right(CheckRelationshipV3Response.authorised(userAssigned = false))
      unassignedConnector.calls.map(_.userId).toSeq shouldBe Seq(Some(userId), None)

      val unrelatedConnector = StubConnector(Right(false), Right(false))
      CheckRelationshipV3Service(unrelatedConnector)
        .checkRelationship(arn, userId, client)
        .futureValue shouldBe Right(CheckRelationshipV3Response.NotAuthorised)

    "check MTD-IT main, supporting and IR-SA in order and stop on the first relationship" in:
      val client = serviceClient("MTD-IT", "AB123456A")

      val mainConnector = StubConnector(Right(true))
      CheckRelationshipV3Service(mainConnector)
        .checkRelationship(arn, userId, client)
        .futureValue shouldBe Right(
        CheckRelationshipV3Response.authorised(userAssigned = true, Some(RelationshipAgentType.Main))
      )
      mainConnector.calls.map(_.service).toSeq shouldBe Seq("HMRC-MTD-IT")

      val supportingConnector = StubConnector(Right(false), Right(false), Right(true))
      CheckRelationshipV3Service(supportingConnector)
        .checkRelationship(arn, userId, client)
        .futureValue shouldBe Right(
        CheckRelationshipV3Response.authorised(userAssigned = true, Some(RelationshipAgentType.Supporting))
      )
      supportingConnector.calls.map(_.service).toSeq shouldBe Seq(
        "HMRC-MTD-IT",
        "HMRC-MTD-IT",
        "HMRC-MTD-IT-SUPP"
      )

      val signupConnector = StubConnector(Right(false), Right(false), Right(false), Right(false), Right(true))
      CheckRelationshipV3Service(signupConnector)
        .checkRelationship(arn, userId, client)
        .futureValue shouldBe Right(
        CheckRelationshipV3Response.authorised(userAssigned = true, Some(RelationshipAgentType.MtdSignup))
      )
      signupConnector.calls.map(_.service).toSeq shouldBe Seq(
        "HMRC-MTD-IT",
        "HMRC-MTD-IT",
        "HMRC-MTD-IT-SUPP",
        "HMRC-MTD-IT-SUPP",
        "IR-SA"
      )
      signupConnector.calls.last.userId shouldBe None

    "return the main or supporting agent type when the relationship exists but the user is unassigned" in:
      val client = serviceClient("MTD-IT", "AB123456A")

      val mainConnector = StubConnector(Right(false), Right(true))
      CheckRelationshipV3Service(mainConnector)
        .checkRelationship(arn, userId, client)
        .futureValue shouldBe Right(
        CheckRelationshipV3Response.authorised(userAssigned = false, Some(RelationshipAgentType.Main))
      )

      val supportingConnector = StubConnector(Right(false), Right(false), Right(false), Right(true))
      CheckRelationshipV3Service(supportingConnector)
        .checkRelationship(arn, userId, client)
        .futureValue shouldBe Right(
        CheckRelationshipV3Response.authorised(userAssigned = false, Some(RelationshipAgentType.Supporting))
      )

    "stop and return an unexpected downstream error" in:
      val connector = StubConnector(Left(StandardInternalServerError))

      CheckRelationshipV3Service(connector)
        .checkRelationship(arn, userId, serviceClient("MTD-IT", "AB123456A"))
        .futureValue shouldBe Left(StandardInternalServerError)
      connector.calls should have size 1
