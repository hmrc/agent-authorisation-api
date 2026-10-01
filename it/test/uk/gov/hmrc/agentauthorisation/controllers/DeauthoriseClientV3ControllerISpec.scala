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

package uk.gov.hmrc.agentauthorisation.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.agentauthorisation.models.ApiService.{MtdIt, MtdVat}
import uk.gov.hmrc.agentauthorisation.models.{ApiClientId, ApiVersion, LockedRequest}
import uk.gov.hmrc.agentauthorisation.support.BaseISpec

class DeauthoriseClientV3ControllerISpec extends BaseISpec:

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> true)

  private lazy val controller = app.injector.instanceOf[DeauthoriseClientController]
  private val requestBase =
    FakeRequest("PUT", s"/agents/${arn.value}/deauthorise-client")
      .withHeaders("Accept" -> ApiVersion.V3AcceptHeader, "Authorization" -> "Bearer XYZ")

  private val jsonBodyITSA: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientType": "personal", "clientIdType": "ni", "clientId": "${validNino.value}"}"""
  )

  private val jsonBodyVAT: JsValue = Json.parse(
    s"""{"service": ["MTD-VAT"], "clientType": "business", "clientIdType": "vrn", "clientId": "${validVrn.value}"}"""
  )

  "PUT /agents/:arn/deauthorise-client for V3" should:
    "return 204 when the ITSA relationship is successfully removed" in:
      givenRemoveAuthorisationStub(
        arn = arn,
        clientId = validNino.value,
        service = "HMRC-MTD-IT",
        status = NO_CONTENT
      )

      val result: Result =
        controller.deauthoriseRelationshipV3(arn, ApiClientId.from(validNino.value).get, MtdIt)(
          authorisedAsValidAgent(requestBase.withJsonBody(jsonBodyITSA), arn.value)
        ).futureValue

      status(result).shouldBe(NO_CONTENT)
      result.body.isKnownEmpty.shouldBe(true)

    "return 204 when no ITSA relationship exists" in:
      givenRemoveAuthorisationStub(
        arn = arn,
        clientId = validNino.value,
        service = "HMRC-MTD-IT",
        status = NOT_FOUND,
        optCode = Some("RELATIONSHIP_NOT_FOUND")
      )

      val result: Result =
        controller.deauthoriseRelationshipV3(arn, ApiClientId.from(validNino.value).get, MtdIt)(
          authorisedAsValidAgent(requestBase.withJsonBody(jsonBodyITSA), arn.value)
        ).futureValue

      status(result).shouldBe(NO_CONTENT)
      result.body.isKnownEmpty.shouldBe(true)

    "return 204 when the VAT relationship is successfully removed" in:
      givenRemoveAuthorisationStub(
        arn = arn,
        clientId = validVrn.value,
        service = "HMRC-MTD-VAT",
        status = NO_CONTENT
      )

      val result: Result =
        controller.deauthoriseRelationshipV3(arn, ApiClientId.from(validVrn.value).get, MtdVat)(
          authorisedAsValidAgent(requestBase.withJsonBody(jsonBodyVAT), arn.value)
        ).futureValue

      status(result).shouldBe(NO_CONTENT)
      result.body.isKnownEmpty.shouldBe(true)

    "return 403 ALREADY_BEING_PROCESSED when ACR reports the request is already being processed" in:
      givenRemoveAuthorisationStub(
        arn = arn,
        clientId = validNino.value,
        service = "HMRC-MTD-IT",
        status = FORBIDDEN,
        optCode = Some("ALREADY_BEING_PROCESSED")
      )

      val result: Result =
        controller.deauthoriseRelationshipV3(arn, ApiClientId.from(validNino.value).get, MtdIt)(
          authorisedAsValidAgent(requestBase.withJsonBody(jsonBodyITSA), arn.value)
        ).futureValue

      status(result).shouldBe(FORBIDDEN)
      contentAsJson(result).shouldBe(LockedRequest.toJson)
