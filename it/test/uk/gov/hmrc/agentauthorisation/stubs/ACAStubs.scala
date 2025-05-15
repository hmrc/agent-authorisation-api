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
import uk.gov.hmrc.agentauthorisation.UriPathEncoding.encodePathSegment
import uk.gov.hmrc.agentauthorisation.support.{TestIdentifiers, WireMockSupport}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, Vrn}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

trait ACAStubs {
  me: WireMockSupport with TestIdentifiers =>

  def givenMatchingClientIdAndPostcode(nino: Nino, postcode: String) =
    stubFor(
      get(urlEqualTo(s"/agent-client-authorisation/known-facts/individuals/nino/${nino.value}/sa/postcode/$postcode"))
        .willReturn(
          aResponse()
            .withStatus(204)
        )
    )

  def givenNonMatchingClientIdAndPostcode(nino: Nino, postcode: String) =
    stubFor(
      get(urlEqualTo(s"/agent-client-authorisation/known-facts/individuals/nino/${nino.value}/sa/postcode/$postcode"))
        .willReturn(
          aResponse()
            .withStatus(403)
            .withBody(s"""
                         |{
                         |   "code":"POSTCODE_DOES_NOT_MATCH",
                         |   "message":"The submitted postcode did not match the client's postcode as held by HMRC."
                         |}
           """.stripMargin)
        )
    )

  def givenNotEnrolledClientITSA(nino: Nino, postcode: String) =
    stubFor(
      get(urlEqualTo(s"/agent-client-authorisation/known-facts/individuals/nino/${nino.value}/sa/postcode/$postcode"))
        .willReturn(
          aResponse()
            .withStatus(403)
            .withBody(s"""
                         |{
                         |   "code":"CLIENT_REGISTRATION_NOT_FOUND",
                         |   "message":"The Client's MTDfB registration was not found."
                         |}
           """.stripMargin)
        )
    )

  def checkClientIdAndVatRegDate(vrn: Vrn, date: LocalDate, responseStatus: Int, clientInsolvent: Boolean = false) = {
    val responseCode =
      if (responseStatus == 403)
        if (clientInsolvent) "VAT_RECORD_CLIENT_INSOLVENT_TRUE" else "VAT_REGISTRATION_DATE_DOES_NOT_MATCH"
      else ""
    stubFor(
      get(
        urlEqualTo(
          s"/agent-client-authorisation/known-facts/organisations/vat/${vrn.value}/registration-date/${date.toString}"
        )
      )
        .willReturn(
          aResponse()
            .withStatus(responseStatus)
            .withBody(
              s"""{
                 |"code": "$responseCode"
                 |}""".stripMargin
            )
        )
    )
  }

  def verifyCheckVatRegisteredClientStubAttempt(vrn: Vrn, date: LocalDate): Unit = {
    val vrnEncoded = encodePathSegment(vrn.value)
    val dateEncoded = encodePathSegment(date.toString)
    verify(
      1,
      getRequestedFor(
        urlEqualTo(
          s"/agent-client-authorisation/known-facts/organisations/vat/$vrnEncoded/registration-date/$dateEncoded"
        )
      )
    )
  }

  def verifyCheckItsaRegisteredClientStubAttempt(nino: Nino, postcode: String): Unit = {
    val ninoEncoded = encodePathSegment(nino.value)
    val postEncoded = encodePathSegment(postcode)
    verify(
      1,
      getRequestedFor(
        urlEqualTo(s"/agent-client-authorisation/known-facts/individuals/nino/$ninoEncoded/sa/postcode/$postEncoded")
      )
    )
  }

  def verifyNoCheckVatRegisteredClientStubAttempt(): Unit =
    verify(
      0,
      getRequestedFor(urlPathMatching("/agent-client-authorisation/known-facts/organisations/.*/registration-date/.*"))
    )

  def givenInvitationNotFound(arn: Arn, invitationId: InvitationId): Unit =
    stubFor(
      get(urlEqualTo(s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
    )

}
