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

package uk.gov.hmrc.agentauthorisation.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec

class DeauthoriseClientControllerISpec extends BaseISpec {

  lazy val controller: DeauthoriseClientController = app.injector.instanceOf[DeauthoriseClientController]

  private val requestBase =
    FakeRequest("PUT", s"/agents/${arn.value}/deauthorise-client")
      .withHeaders("Accept" -> s"application/vnd.hmrc.2.0+json", "Authorization" -> "Bearer XYZ")

  private val jsonBodyITSA: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientType": "personal", "clientIdType": "ni", "clientId": "${validNino.value}"}"""
  )

  private val jsonBodyVAT: JsValue = Json.parse(
    s"""{"service": ["MTD-VAT"], "clientType": "business", "clientIdType": "vrn", "clientId": "${validVrn.value}"}"""
  )

  "PUT /agents/:arn/deauthorise-client" should {

    "return 204 when the ITSA relationship is successfully removed" in {
      givenRemoveAuthorisationStub(
        arn = arn,
        clientId = validNino.value,
        service = "HMRC-MTD-IT",
        status = NO_CONTENT
      )

      val deauthoriseRelationship = controller.deauthoriseRelationship(arn)

      val result: Result =
        deauthoriseRelationship(
          authorisedAsValidAgent(requestBase.withJsonBody(jsonBodyITSA), arn.value)
        ).futureValue

      status(result) shouldBe NO_CONTENT
      result.body.isKnownEmpty shouldBe true
    }

    "return 204 when the VAT relationship is successfully removed" in {
      givenRemoveAuthorisationStub(
        arn = arn,
        clientId = validVrn.value,
        service = "HMRC-MTD-VAT",
        status = NO_CONTENT
      )

      val deauthoriseRelationship = controller.deauthoriseRelationship(arn)

      val result: Result =
        deauthoriseRelationship(
          authorisedAsValidAgent(requestBase.withJsonBody(jsonBodyVAT), arn.value)
        ).futureValue

      status(result) shouldBe NO_CONTENT
      result.body.isKnownEmpty shouldBe true
    }

    "return 403 NO_RELATIONSHIP when ACR reports RELATIONSHIP_NOT_FOUND" in {
      givenRemoveAuthorisationStub(
        arn = arn,
        clientId = validNino.value,
        service = "HMRC-MTD-IT",
        status = NOT_FOUND,
        optCode = Some("RELATIONSHIP_NOT_FOUND")
      )

      val deauthoriseRelationship = controller.deauthoriseRelationship(arn)

      val result: Result =
        deauthoriseRelationship(
          authorisedAsValidAgent(requestBase.withJsonBody(jsonBodyITSA), arn.value)
        ).futureValue

      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe NoRelationship.toJson
    }

    "return 400 CLIENT_TYPE_NOT_SUPPORTED when clientType is invalid for the service" in {
      val invalidClientTypePayload: JsValue =
        Json.parse(
          s"""{"service": ["MTD-IT"], "clientType": "business", "clientIdType": "ni", "clientId": "${validNino.value}"}"""
        )

      val deauthoriseRelationship = controller.deauthoriseRelationship(arn)

      val result: Result =
        deauthoriseRelationship(
          authorisedAsValidAgent(requestBase.withJsonBody(invalidClientTypePayload), arn.value)
        ).futureValue

      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe UnsupportedClientType.toJson
    }
  }
}

