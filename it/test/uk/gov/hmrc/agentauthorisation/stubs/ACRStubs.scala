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

package uk.gov.hmrc.agentauthorisation.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.Eventually.eventually
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.agentauthorisation.models.{ApiErrorResponse, Service}
import uk.gov.hmrc.agentauthorisation.support.{TestIdentifiers, WireMockSupport}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, Vrn}
import uk.gov.hmrc.domain.Nino

trait ACRStubs {
  me: WireMockSupport with TestIdentifiers =>

  def createInvitationStub(
    arn: Arn,
    invitationId: InvitationId,
    service: Service,
    clientId: String,
    knownFact: String,
    clientType: String
  ): Unit = {
    val requestBody = Json.obj(
      "service"          -> service.internalServiceName,
      "suppliedClientId" -> clientId,
      "knownFact"        -> knownFact,
      "clientType"       -> clientType
    )
    val responseBody = Json.obj(
      "invitationId" -> invitationId.value
    )
    stubFor(
      post(urlEqualTo(s"/agent-client-relationships/api/${arn.value}/invitation"))
        .withRequestBody(equalToJson(requestBody.toString()))
        .willReturn(
          jsonResponse(
            responseBody.toString(),
            201
          )
        )
    )
  }

  def createInvitationErrorStub(
    error: ApiErrorResponse,
    arn: Arn,
    invitationId: InvitationId,
    service: Service,
    clientId: String,
    knownFact: String,
    clientType: String
  ): Unit = {
    val requestBody = Json.obj(
      "service"          -> service.internalServiceName,
      "suppliedClientId" -> clientId,
      "knownFact"        -> knownFact,
      "clientType"       -> clientType
    )
    val responseBody = Json.obj(
      "code"         -> error.code,
      "invitationId" -> invitationId.value
    )
    stubFor(
      post(urlEqualTo(s"/agent-client-relationships/api/${arn.value}/invitation"))
        .withRequestBody(equalToJson(requestBody.toString()))
        .willReturn(
          jsonResponse(
            responseBody.toString(),
            error.statusCode
          )
        )
    )
  }

  def getStatusRelationshipItsa(arn: String, nino: Nino, status: Int, service: String): Unit =
    stubFor(
      get(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/$service/client/NI/${nino.value}"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def verifyStatusRelationshipItsaEventWasSent(arn: String, nino: Nino, service: String) = eventually {
    verify(
      1,
      getRequestedFor((urlEqualTo(s"/agent-client-relationships/agent/$arn/service/$service/client/NI/${nino.value}")))
    )
  }

  def verifyNoStatusRelationshipItsaEventWasSent(arn: String, nino: Nino, service: String): Unit =
    verify(
      0,
      getRequestedFor((urlEqualTo(s"/agent-client-relationships/agent/$arn/service/$service/client/NI/${nino.value}")))
    )

  def getStatusRelationshipVat(arn: String, vrn: Vrn, status: Int): Unit =
    stubFor(
      get(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-MTD-VAT/client/VRN/${vrn.value}"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def givenGetITSAInvitationStub(arn: Arn, status: String): Unit =
    givenGetAgentInvitationStub(arn, invitationIdITSA, serviceITSA, status)

  def givenGetITSASuppInvitationStub(arn: Arn, status: String): Unit =
    givenGetAgentInvitationStub(arn, invitationIdITSA, serviceITSASupp, status)

  def givenGetVATInvitationStub(arn: Arn, status: String): Unit =
    givenGetAgentInvitationStub(arn, invitationIdVAT, serviceVAT, status)

  def givenGetAgentInvitationStub(
    arn: Arn,
    invitationId: InvitationId,
    service: String,
    status: String
  ): Unit =
    stubFor(
      get(urlEqualTo(s"/agent-client-relationships/api/${arn.value}/invitation/${invitationId.value}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              Json
                .obj(
                  "uid"                 -> "12345678",
                  "normalizedAgentName" -> "agent-1",
                  "created"             -> "2017-10-31T23:22:50.971Z",
                  "service"             -> service,
                  "status"              -> status,
                  "expiresOn"           -> "2017-12-18",
                  "invitationId"        -> invitationId.value,
                  "lastUpdated"         -> "2018-09-11T21:02:50.123Z"
                )
                .toString()
            )
        )
    )

  def givenGetAgentInvitationStubReturns(
    arn: Arn,
    invitationId: InvitationId,
    status: Int,
    optCode: Option[String]
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-client-relationships/api/${arn.value}/invitation/${invitationId.value}"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(optCode.fold(Json.obj())(code => Json.obj("code" -> code)).toString())
        )
    )

  def pendingItsaInvitation(service: Service): JsObject = Json.obj(
    "_links"          -> Json.obj("self" -> Json.obj("href" -> "/agents/TARN0000001/invitations/ABERULMHCKKW3")),
    "created"         -> "2017-10-31T23:22:50.971Z",
    "expiresOn"       -> "2017-12-18T00:00:00.000",
    "arn"             -> "TARN0000001",
    "service"         -> Json.arr("MTD-IT"),
    "status"          -> "Pending",
    "clientActionUrl" -> "http://localhost:9435/appoint-someone-to-deal-with-HMRC-for-you/12345678/agent-1/income-tax",
    "agentType"       -> service.agentType
  )

  def respondedItsaInvitation(service: Service): JsObject = Json.obj(
    "_links"    -> Json.obj("self" -> Json.obj("href" -> "/agents/TARN0000001/invitations/ABERULMHCKKW3")),
    "created"   -> "2017-10-31T23:22:50.971Z",
    "updated"   -> "2018-09-11T21:02:50.123Z",
    "arn"       -> "TARN0000001",
    "service"   -> Json.arr("MTD-IT"),
    "status"    -> "Accepted",
    "agentType" -> service.agentType
  )

  val pendingVatInvitation: JsObject = Json.obj(
    "_links"          -> Json.obj("self" -> Json.obj("href" -> "/agents/TARN0000001/invitations/CZTW1KY6RTAAT")),
    "created"         -> "2017-10-31T23:22:50.971Z",
    "expiresOn"       -> "2017-12-18T00:00:00.000",
    "arn"             -> "TARN0000001",
    "service"         -> Json.arr("MTD-VAT"),
    "status"          -> "Pending",
    "clientActionUrl" -> "http://localhost:9435/appoint-someone-to-deal-with-HMRC-for-you/12345678/agent-1/vat"
  )

  val respondedVatInvitation: JsObject = Json.obj(
    "_links"  -> Json.obj("self" -> Json.obj("href" -> "/agents/TARN0000001/invitations/CZTW1KY6RTAAT")),
    "created" -> "2017-10-31T23:22:50.971Z",
    "updated" -> "2018-09-11T21:02:50.123Z",
    "arn"     -> "TARN0000001",
    "service" -> Json.arr("MTD-VAT"),
    "status"  -> "Accepted"
  )
  def givenCancelAgentInvitationStub(invitationId: InvitationId, status: Int) =
    stubFor(
      put(
        urlEqualTo(s"/agent-client-relationships/agent/cancel-invitation/${invitationId.value}")
      )
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def givenCancelAgentInvitationStubInvalid(error: ApiErrorResponse, invitationId: InvitationId) =
    stubFor(
      put(
        urlEqualTo(s"/agent-client-relationships/agent/cancel-invitation/${invitationId.value}")
      )
        .willReturn(
          jsonResponse(
            error.toJson.toString(),
            error.statusCode
          )
        )
    )

}
