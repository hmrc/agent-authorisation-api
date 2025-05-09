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

package uk.gov.hmrc.agentauthorisation.controllers.api.agent

import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.agentauthorisation.models.Service.{ItsaMain, ItsaSupp}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class CreateInvitationControllerISpec extends BaseISpec {

  lazy val controller: CreateInvitationController = app.injector.instanceOf[CreateInvitationController]

  lazy val configuration: Configuration = app.injector.instanceOf[Configuration]

  private def stubCreateItsaInAcr(error: Option[ApiErrorResponse], main: Boolean): Unit = {
    val service = if (main) ItsaMain else ItsaSupp
    if (error.isDefined) {
      createInvitationErrorStub(
        error.get,
        arn,
        invitationIdITSA,
        service,
        validNino.value,
        validPostcode,
        "personal"
      )
    } else {
      createInvitationStub(
        arn,
        invitationIdITSA,
        service,
        validNino.value,
        validPostcode,
        "personal"
      )
    }
  }

  private def stubCreateVatInAcr(error: Option[ApiErrorResponse] = None): Unit =
    if (error.isDefined) {
      createInvitationErrorStub(
        error.get,
        arn,
        invitationIdVAT,
        Service.Vat,
        validVrn.value,
        validVatRegDate,
        "business"
      )
    } else {
      createInvitationStub(
        arn,
        invitationIdVAT,
        Service.Vat,
        validVrn.value,
        validVatRegDate,
        "business"
      )
    }

  val itsaPayload: CreateInvitationPayload = CreateInvitationPayload(
    service = List("MTD-IT"),
    clientType = "personal",
    clientIdType = "ni",
    clientId = validNino.value,
    knownFact = validPostcode,
    agentType = Some("main")
  )

  val jsonBodyITSA: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}"""
  )

  val jsonBodyITSASupportingAgentType: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode", "agentType":"supporting"}"""
  )

  val jsonBodyITSAMainAgentType: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode", "agentType":"main"}"""
  )

  val jsonBodyITSAInvalidAgentType: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientType":"personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode", "agentType":"xxxx"}"""
  )

  val jsonBodyVAT: JsValue = Json.parse(
    s"""{"service": ["MTD-VAT"], "clientType":"business", "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "$validVatRegDate"}"""
  )

  "POST /agents/:arn/invitations" should {

    val request = FakeRequest("POST", s"/agents/${arn.value}/invitations")
      .withHeaders("Accept" -> s"application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer XYZ")
    val createInvitation = controller.createInvitation(arn)

    "return 204 when invitation is successfully created for ITSA without an agentType" in {
      stubCreateItsaInAcr(error = None, main = true)
      val result = createInvitation(
        authorisedAsValidAgent(request.withJsonBody(Json.toJson(itsaPayload.copy(agentType = None))), arn.value)
      )
      status(result) shouldBe 204
      header("Location", result) shouldBe Some("/agents/TARN0000001/invitations/ABERULMHCKKW3")
    }

    "return 204 when invitation is successfully created for ITSA with a supporting agent" in {
      stubCreateItsaInAcr(error = None, main = false)
      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSASupportingAgentType), arn.value))
      status(result) shouldBe 204
      header("Location", result) shouldBe Some("/agents/TARN0000001/invitations/ABERULMHCKKW3")
    }

    "return 204 when invitation is successfully created for ITSA with a main agent" in {
      stubCreateItsaInAcr(error = None, main = true)
      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(Json.toJson(itsaPayload)), arn.value))
      status(result) shouldBe 204
      header("Location", result) shouldBe Some("/agents/TARN0000001/invitations/ABERULMHCKKW3")
    }

    "return 204 when invitation is successfully created for VAT" in {
      stubCreateVatInAcr()
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))
      status(result) shouldBe 204
      header("Location", result) shouldBe Some("/agents/TARN0000001/invitations/CZTW1KY6RTAAT")
    }

    "return 400 when invitation is requested for ITSA with invalid agent" in {
      val result =
        createInvitation(
          authorisedAsValidAgent(request.withJsonBody(jsonBodyITSAInvalidAgentType), arn.value)
        ).futureValue
      status(result) shouldBe 400
      contentAsJson(result) shouldBe UnsupportedAgentType.toJson
    }

    "return 400 SERVICE_NOT_SUPPORTED when the service is not supported" in {
      val jsonBodyInvalidService = Json.parse(
        s"""{"service": ["foo"], "clientType": "personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}"""
      )
      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value)).futureValue
      status(result) shouldBe 400
      contentAsJson(result) shouldBe UnsupportedService.toJson
    }

    "return 400 CLIENT_TYPE_NOT_SUPPORTED when the client type is not valid" in {
      val result =
        createInvitation(
          authorisedAsValidAgent(request.withJsonBody(Json.toJson(itsaPayload.copy(clientType = "trust"))), arn.value)
        ).futureValue
      status(result) shouldBe 400
      contentAsJson(result) shouldBe UnsupportedClientType.toJson
    }

    "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for ITSA" in {
      val jsonBodyInvalidClientId = Json.parse(
        s"""{"service": ["MTD-IT"], "clientType": "personal", "clientIdType": "ni", "clientId": "foo", "knownFact": "$validPostcode"}"""
      )
      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value)).futureValue
      status(result) shouldBe 400
      contentAsJson(result) shouldBe ClientIdInvalidFormat.toJson
    }

    "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for VAT" in {
      val jsonBodyInvalidClientId = Json.parse(
        s"""{"service": ["MTD-VAT"], "clientType": "business", "clientIdType": "vrn", "clientId": "foo", "knownFact": "$validVatRegDate"}"""
      )
      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value)).futureValue
      status(result) shouldBe 400
      contentAsJson(result) shouldBe ClientIdInvalidFormat.toJson
    }

    "return 400 POSTCODE_FORMAT_INVALID when the postcode has an invalid format" in {
      val jsonBodyInvalidPostcode = Json.parse(
        s"""{"service": ["MTD-IT"], "clientType": "personal", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "foo"}"""
      )
      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidPostcode), arn.value)).futureValue
      status(result) shouldBe 400
      contentAsJson(result) shouldBe PostcodeFormatInvalid.toJson
    }

    "return 400 VAT_REG_DATE_FORMAT_INVALID when the VAT registration date has an invalid format" in {
      val jsonBodyInvalidVatRegDate = Json.parse(
        s"""{"service": ["MTD-VAT"], "clientType": "business", "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "foo"}"""
      )
      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidVatRegDate), arn.value)).futureValue
      status(result) shouldBe 400
      contentAsJson(result) shouldBe VatRegDateFormatInvalid.toJson
    }

    "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for ITSA" in {
      val jsonBodyClientIdNotMatchService = Json.parse(
        s"""{"service": ["MTD-IT"], "clientType": "personal", "clientIdType": "ni", "clientId": "${validVrn.value}", "knownFact": "foo"}"""
      )
      val result =
        createInvitation(
          authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value)
        ).futureValue
      status(result) shouldBe 400
      contentAsJson(result) shouldBe ClientIdDoesNotMatchService.toJson
    }

    "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for VAT" in {
      val jsonBodyClientIdNotMatchService = Json.parse(
        s"""{"service": ["MTD-VAT"], "clientType": "business", "clientIdType": "vrn", "clientId": "${validNino.value}", "knownFact": "foo"}"""
      )
      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value))
      status(result) shouldBe 400
      await(result) shouldBe ClientIdDoesNotMatchServiceResult
    }

    "return 403 ALREADY_PROCESSING when lock cannot be acquired" in {

      val mongoLockRepository = app.injector.instanceOf[MongoLockRepository]

      mongoLockRepository.takeLock(
        lockId = s"create-invitation-${arn.value}-HMRC-MTD-VAT-${validVrn.value}",
        owner = "34a34ff7-2a7d-4695-8d4a-3f210fb03686",
        ttl = 10.seconds
      )
      val result: Result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value)).futureValue
      status(result) shouldBe 403
      contentAsJson(result) shouldBe LockedRequest.toJson
    }

    "return 403 CLIENT_REGISTRATION_NOT_FOUND when no registration found" in {
      stubCreateItsaInAcr(error = Some(ClientRegistrationNotFound), main = true)
      val result: Result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value)).futureValue
      status(result) shouldBe 403
      contentAsJson(result) shouldBe ClientRegistrationNotFound.toJson
    }

    "return 403 POSTCODE_DOES_NOT_MATCH when the postcode and clientId do not match" in {
      stubCreateItsaInAcr(error = Some(PostcodeDoesNotMatch), main = true)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value)).futureValue
      status(result) shouldBe 403
      contentAsJson(result) shouldBe PostcodeDoesNotMatch.toJson
    }

    "return 403 VAT_REG_DATE_DOES_NOT_MATCH when the VAT registration date and clientId do not match" in {
      stubCreateVatInAcr(error = Some(VatRegDateDoesNotMatch))
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value)).futureValue
      status(result) shouldBe 403
      contentAsJson(result) shouldBe VatRegDateDoesNotMatch.toJson
    }

    "return 403 NOT_AN_AGENT when the logged in user does not have an HMRC-AS-AGENT enrolment" in {
      givenUnauthorisedForInsufficientEnrolments()
      val result = createInvitation(request.withJsonBody(jsonBodyITSA)).futureValue
      status(result) shouldBe 403
      contentAsJson(result) shouldBe NotAnAgent.toJson
    }

    "return 403 NO_PERMISSION_ON_AGENCY when the logged in user does not have an HMRC-AS-AGENT enrolment" in {
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn2.value)).futureValue
      status(result) shouldBe 403
      contentAsJson(result) shouldBe NoPermissionOnAgency.toJson
    }

    "return 403 DUPLICATE_AUTHORISATION_REQUEST when there is already a pending invitation" in {
      stubCreateItsaInAcr(error = Some(DuplicateAuthorisationRequest(invitationIdITSA)), main = true)
      val result: Future[Result] =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))
      status(result) shouldBe 403
      header("Location", result) shouldBe Some(s"/agents/${arn.value}/invitations/${invitationIdITSA.value}")
      contentAsJson(result.futureValue) shouldBe DuplicateAuthorisationRequest(invitationIdITSA).toJson
    }

    "return 403 ALREADY_AUTHORISED when there is already an active relationship" in {
      stubCreateItsaInAcr(error = Some(AlreadyAuthorised), main = true)
      val result: Result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value)).futureValue
      status(result) shouldBe 403
      contentAsJson(result) shouldBe AlreadyAuthorised.toJson
    }

    "return 403 VAT_CLIENT_INSOLVENT when the VAT customer is insolvent" in {
      stubCreateVatInAcr(error = Some(VatClientInsolvent))
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value)).futureValue
      status(result) shouldBe 403
      contentAsJson(result) shouldBe VatClientInsolvent.toJson
    }

  }
}
