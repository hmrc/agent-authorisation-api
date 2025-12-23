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

package uk.gov.hmrc.agentauthorisation.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.models.Service.{ItsaMain, ItsaSupp, Vat}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec

class CheckRelationshipControllerISpec extends BaseISpec {

  lazy val controller: CheckRelationshipController = app.injector.instanceOf[CheckRelationshipController]

  val jsonBodyITSA: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}"""
  )

  val itsaClientAccessData = ClientAccessData(
    service = ItsaMain,
    suppliedClientId = validNino.value,
    knownFact = validPostcode,
    clientType = None
  )

  val jsonBodyITSASupportingAgentType: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode", "agentType":"supporting"}"""
  )

  val jsonBodyITSAMainAgentType: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode", "agentType":"main"}"""
  )

  val jsonBodyITSAInvalidAgentType: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode", "agentType":"xxxx"}"""
  )

  val jsonBodyVAT: JsValue = Json.parse(
    s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "$validVatRegDate"}"""
  )

  val vatClientAccessData = ClientAccessData(
    service = Vat,
    suppliedClientId = validVrn.value,
    knownFact = validVatRegDate,
    clientType = None
  )

  val jsonBodyVATAgentType: JsValue = Json.parse(
    s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "$validVatRegDate", "agentType":"main"}"""
  )

  "POST /agents/:arn/relationships" when {

    "getting the status of an ITSA relationship" should {
      val checkRelationshipApi = controller.checkRelationship(arn)
      val request = FakeRequest("POST", s"/agents/$arn/relationships")
        .withHeaders("Accept" -> s"application/vnd.hmrc.2.0+json", "Authorization" -> "Bearer XYZ")

      "return 204 when the relationship is active for ITSA" in {
        givenCheckRelationshipStub(
          arn = arn.value,
          status = 204,
          optCode = None,
          clientAccessData = itsaClientAccessData
        )
        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value)).futureValue
        status(result) shouldBe 204
        result.body.isKnownEmpty shouldBe true
      }

      "return 204 when the relationship is active for ITSA supporting" in {
        givenCheckRelationshipStub(
          arn = arn.value,
          status = 204,
          optCode = None,
          clientAccessData = itsaClientAccessData.copy(service = ItsaSupp)
        )
        val result =
          checkRelationshipApi(
            authorisedAsValidAgent(request.withJsonBody(jsonBodyITSASupportingAgentType), arn.value)
          ).futureValue
        status(result) shouldBe 204
        result.body.isKnownEmpty shouldBe true
      }

      "return 204 when the relationship is active for VAT" in {
        givenCheckRelationshipStub(
          arn = arn.value,
          status = 204,
          optCode = None,
          clientAccessData = vatClientAccessData
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value)).futureValue
        status(result) shouldBe 204
        result.body.isKnownEmpty shouldBe true
      }

      "return 404 when the relationship is not found for ITSA" in {
        givenCheckRelationshipStub(
          arn = arn.value,
          status = 404,
          optCode = Some("RELATIONSHIP_NOT_FOUND"),
          clientAccessData = itsaClientAccessData
        )

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value)).futureValue
        status(result) shouldBe 404
        result shouldBe RelationshipNotFound.toResult
      }

      "return 404 when the relationship is not found for VAT" in {
        givenCheckRelationshipStub(
          arn = arn.value,
          status = 404,
          optCode = Some("RELATIONSHIP_NOT_FOUND"),
          clientAccessData = vatClientAccessData
        )
        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value)).futureValue
        status(result) shouldBe 404
        result shouldBe RelationshipNotFound.toResult
      }

      "return 400 SERVICE_NOT_SUPPORTED when the service is not supported" in {
        val jsonBodyInvalidService = Json.parse(
          s"""{"service": ["foo"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}"""
        )

        val result =
          checkRelationshipApi(
            authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value)
          ).futureValue

        status(result) shouldBe 400
        result shouldBe UnsupportedService.toResult
      }

      "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for ITSA" in {
        val jsonBodyInvalidClientId = Json.parse(
          s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "foo", "knownFact": "$validPostcode"}"""
        )

        val result =
          checkRelationshipApi(
            authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value)
          ).futureValue

        status(result) shouldBe 400
        result shouldBe ClientIdInvalidFormat.toResult

      }

      "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for VAT" in {
        val jsonBodyInvalidClientId = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "foo", "knownFact": "$validVatRegDate"}"""
        )

        val result =
          checkRelationshipApi(
            authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value)
          ).futureValue

        status(result) shouldBe 400
        result shouldBe ClientIdInvalidFormat.toResult
      }

      "return 400 POSTCODE_FORMAT_INVALID when the postcode has an invalid format" in {
        val jsonBodyInvalidPostcode = Json.parse(
          s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(
            authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidPostcode), arn.value)
          ).futureValue

        status(result) shouldBe 400
        result shouldBe PostcodeFormatInvalid.toResult

      }

      "return 400 VAT_REG_DATE_FORMAT_INVALID when the VAT registration date has an invalid format" in {
        val jsonBodyInvalidVatRegDate = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(
            authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidVatRegDate), arn.value)
          ).futureValue

        status(result) shouldBe 400
        result shouldBe VatRegDateFormatInvalid.toResult

      }

      "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for ITSA" in {
        val jsonBodyClientIdNotMatchService = Json.parse(
          s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validVrn.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(
            authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value)
          ).futureValue

        status(result) shouldBe 400
        result shouldBe ClientIdDoesNotMatchService.toResult

      }

      "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for VAT" in {
        val jsonBodyClientIdNotMatchService = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "${validNino.value}", "knownFact": "foo"}"""
        )

        val result =
          checkRelationshipApi(
            authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value)
          ).futureValue

        status(result) shouldBe 400
        result shouldBe ClientIdDoesNotMatchService.toResult
      }

      "return 403 NOT_AN_AGENT when the logged in user is not have an HMRC-AS-AGENT enrolment" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = checkRelationshipApi(request.withJsonBody(jsonBodyITSA)).futureValue

        status(result) shouldBe 403
        result shouldBe NotAnAgent.toResult
      }

    }
  }

}
