/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.models.{ApiErrorResponse, Arn, DeleteRelationshipPayload}
import uk.gov.hmrc.agentauthorisation.support.BaseSpec
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.concurrent.{ExecutionContext, Future}

class DeleteRelationshipServiceSpec extends BaseSpec with MockFactory {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  private val servicesConfig: ServicesConfig = mock[ServicesConfig]

  (servicesConfig
    .baseUrl(_: String))
    .expects(*)
    .atLeastOnce()
    .returning("http://localhost")

  (servicesConfig
    .getConfString(_: String, _: String))
    .expects(*, *)
    .atLeastOnce()
    .returning("http://localhost")

  (servicesConfig
    .getString(_: String))
    .expects(*)
    .atLeastOnce()
    .returning("PRIVATE")

  private val config = Configuration.apply("api.supported-versions" -> List(1.0))

  private val appConfig = new AppConfig(servicesConfig, config)

  private class StubAcrConnector(expectedService: String, lockService: FakeLockService)
      extends AgentClientRelationshipsConnector(
        httpClient = mock[HttpClientV2],
        metrics = mock[Metrics],
        appConfig = appConfig
      ) {

    var captured: Option[(Arn, String, String)] = None

    override def removeAuthorisation(
      arn: Arn,
      clientId: String,
      service: String
    )(implicit rh: play.api.mvc.RequestHeader): Future[Either[ApiErrorResponse, Unit]] = {
      captured = Some((arn, clientId, service))
      lockService.locked should contain((arn.value, expectedService, clientId))
      Future.successful(Right(()))
    }
  }

  "DeleteRelationshipService" should {

    "use internal ACR service id for MTD-IT when acquiring lock and calling connector" in {
      val lockService = new FakeLockService()
      val acrConnector = new StubAcrConnector("HMRC-MTD-IT", lockService)
      val service = new DeleteRelationshipService(lockService, acrConnector)

      implicit val rh = testRequest(FakeRequest())

      val payload = DeleteRelationshipPayload(
        service = List("MTD-IT"),
        clientType = "personal",
        clientIdType = "ni",
        clientId = "AA123456A",
        agentType = None
      )

      val result = await(service.deleteRelationship(arn, payload))
      result shouldBe Right(())
      acrConnector.captured.value shouldBe ((arn, payload.clientId, "HMRC-MTD-IT"))
      lockService.locked shouldBe empty
    }

    "use internal ACR service id for MTD-VAT when acquiring lock and calling connector" in {
      val lockService = new FakeLockService()
      val acrConnector = new StubAcrConnector("HMRC-MTD-VAT", lockService)
      val service = new DeleteRelationshipService(lockService, acrConnector)

      implicit val rh = testRequest(FakeRequest())

      val payload = DeleteRelationshipPayload(
        service = List("MTD-VAT"),
        clientType = "business",
        clientIdType = "vrn",
        clientId = "101747696",
        agentType = None
      )

      val result = await(service.deleteRelationship(arn, payload))
      result shouldBe Right(())
      acrConnector.captured.value shouldBe ((arn, payload.clientId, "HMRC-MTD-VAT"))
      lockService.locked shouldBe empty
    }

    "use supporting ITSA service id when agentType is supporting" in {
      val lockService = new FakeLockService()
      val acrConnector = new StubAcrConnector("HMRC-MTD-IT-SUPP", lockService)
      val service = new DeleteRelationshipService(lockService, acrConnector)

      implicit val rh = testRequest(FakeRequest())

      val payload = DeleteRelationshipPayload(
        service = List("MTD-IT"),
        clientType = "personal",
        clientIdType = "ni",
        clientId = "AA123456A",
        agentType = Some("supporting")
      )

      val result = await(service.deleteRelationship(arn, payload))
      result shouldBe Right(())
      acrConnector.captured.value shouldBe ((arn, payload.clientId, "HMRC-MTD-IT-SUPP"))
      lockService.locked shouldBe empty
    }
  }
}
