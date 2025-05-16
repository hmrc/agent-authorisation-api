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
import play.api.libs.json.Json
import uk.gov.hmrc.agentauthorisation.models.{ApiErrorResponse, Service}
import uk.gov.hmrc.agentauthorisation.support.{TestIdentifiers, TestInvitation, WireMockSupport}
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

  def givenGetAgentInvitationStub(
    arn: Arn,
    invitation: TestInvitation
  ): Unit =
    stubFor(
      get(urlEqualTo(s"/agent-client-relationships/api/${arn.value}/invitation/${invitation.invitationId.value}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              Json
                .obj(
                  "uid"                 -> "12345678",
                  "normalizedAgentName" -> "agent-1",
                  "created"             -> "2017-10-31T23:22:50.971Z",
                  "service"             -> invitation.service,
                  "status"              -> invitation.status,
                  "expiresOn"           -> "2017-12-18",
                  "invitationId"        -> invitation.invitationId.value,
                  "lastUpdated"         -> "2018-09-11T21:02:50.123Z"
                )
                .toString()
            )
        )
    )

  def givenGetAgentInvitationStubError(
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

  def givenGetAllAgentInvitationsStub(arn: Arn, invitations: Seq[TestInvitation]): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-client-relationships/api/${arn.value}/invitations"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody {
              Json
                .obj(
                  "uid"                 -> "12345678",
                  "normalizedAgentName" -> "agent-1",
                  "invitations" -> invitations.map { invitation =>
                    Json.obj(
                      "created"      -> "2017-10-31T23:22:50.971Z",
                      "service"      -> invitation.service,
                      "status"       -> invitation.status,
                      "expiresOn"    -> "2017-12-18",
                      "invitationId" -> invitation.invitationId.value,
                      "lastUpdated"  -> "2018-09-11T21:02:50.123Z"
                    )
                  }
                )
                .toString()
            }
        )
    )

  def givenGetAllAgentInvitationsStubEmpty(arn: Arn): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-client-relationships/api/${arn.value}/invitations"))
        .willReturn(
          aResponse()
            .withStatus(204)
            .withBody(
              Json.obj().toString()
            )
        )
    )

  def givenGetAgentAllInvitationsStubError(
    arn: Arn,
    status: Int,
    optCode: Option[String]
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-client-relationships/api/${arn.value}/invitations"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(optCode.fold(Json.obj())(code => Json.obj("code" -> code)).toString())
        )
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
