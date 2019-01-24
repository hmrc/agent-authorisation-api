package uk.gov.hmrc.agentauthorisation.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.LocalDate
import uk.gov.hmrc.agentauthorisation.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, Vrn}
import uk.gov.hmrc.agentauthorisation.UriPathEncoding.encodePathSegment
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.agentauthorisation._

trait ACAStubs {
  me: WireMockSupport =>

  def createInvitationStub(
    arn: Arn,
    clientId: String,
    invitationId: InvitationId,
    suppliedClientId: String,
    suppliedClientType: String,
    clientType: String,
    service: String,
    serviceIdentifier: String,
    knownFact: String): Unit =
    stubFor(
      post(urlEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .withRequestBody(equalToJson(s"""
                                        |{
                                        |   "service": "$service",
                                        |   "clientType": "$clientType",
                                        |   "clientIdType": "$suppliedClientType",
                                        |   "clientId":"$suppliedClientId",
                                        |   "knownFact":"$knownFact"
                                        |}""".stripMargin))
        .willReturn(
          aResponse()
            .withStatus(201)
            .withHeader(
              "InvitationId",
              invitationId.value
            )))

  def failedCreateInvitation(arn: Arn): Unit =
    stubFor(
      post(urlEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .willReturn(aResponse()
          .withStatus(400)))

  def createAgentLink(clientType: String, normalisedAgentName: String): Unit =
    stubFor(
      post(urlEqualTo(s"/agent-client-authorisation/agencies/references/arn/${arn.value}/clientType/$clientType"))
        .willReturn(aResponse()
            .withStatus(201)
            .withHeader("location", s"/invitations/$clientType/12345678/$normalisedAgentName")
        )
    )

  def givenMatchingClientIdAndPostcode(nino: Nino, postcode: String) =
    stubFor(
      get(urlEqualTo(s"/agent-client-authorisation/known-facts/individuals/nino/${nino.value}/sa/postcode/$postcode"))
        .willReturn(aResponse()
          .withStatus(204)))

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
           """.stripMargin)))

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
           """.stripMargin)))

  def checkClientIdAndVatRegDate(vrn: Vrn, date: LocalDate, responseStatus: Int) =
    stubFor(
      get(urlEqualTo(
        s"/agent-client-authorisation/known-facts/organisations/vat/${vrn.value}/registration-date/${date.toString}"))
        .willReturn(aResponse()
          .withStatus(responseStatus)))

  def verifyCheckVatRegisteredClientStubAttempt(vrn: Vrn, date: LocalDate): Unit = {
    val vrnEncoded = encodePathSegment(vrn.value)
    val dateEncoded = encodePathSegment(date.toString)
    verify(
      1,
      getRequestedFor(
        urlEqualTo(
          s"/agent-client-authorisation/known-facts/organisations/vat/$vrnEncoded/registration-date/$dateEncoded")))
  }

  def verifyCheckItsaRegisteredClientStubAttempt(nino: Nino, postcode: String): Unit = {
    val ninoEncoded = encodePathSegment(nino.value)
    val postEncoded = encodePathSegment(postcode)
    verify(
      1,
      getRequestedFor(
        urlEqualTo(s"/agent-client-authorisation/known-facts/individuals/nino/$ninoEncoded/sa/postcode/$postEncoded")))
  }

  def verifyNoCheckVatRegisteredClientStubAttempt(): Unit =
    verify(
      0,
      getRequestedFor(urlPathMatching("/agent-client-authorisation/known-facts/organisations/.*/registration-date/.*")))

  def givenGetITSAInvitationStub(arn: Arn, status: String): Unit =
    givenGetAgentInvitationStub(arn, "personal", "ni", validNino.value, invitationIdITSA, serviceITSA, status)

  def givenGetVATInvitationStub(arn: Arn, status: String): Unit =
    givenGetAgentInvitationStub(arn, "business", "vrn", validVrn.value, invitationIdVAT, serviceVAT, status)

  def givenGetAgentInvitationStub(
    arn: Arn,
    clientType: String,
    clientIdType: String,
    clientId: String,
    invitationId: InvitationId,
    service: String,
    status: String): Unit =
    stubFor(
      get(urlEqualTo(s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "arn" : "${arn.value}",
                         |  "service" : "$service",
                         |  "clientType":"$clientType",
                         |  "clientId" : "$clientId",
                         |  "clientIdType" : "$clientIdType",
                         |  "suppliedClientId" : "$clientId",
                         |  "suppliedClientIdType" : "$clientIdType",
                         |  "status" : "$status",
                         |  "created" : "2017-10-31T23:22:50.971Z",
                         |  "lastUpdated" : "2018-09-11T21:02:00.000Z",
                         |  "expiryDate" : "2017-12-18",
                         |  "_links": {
                         |    	"self" : {
                         |			  "href" : "$wireMockBaseUrlAsString/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"
                         |		  }
                         |  }
                         |}""".stripMargin)))

  def givenAllInvitationsPendingStub(arn: Arn): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .withQueryParam("createdOnOrAfter", equalTo(LocalDate.now.minusDays(30).toString("yyyy-MM-dd")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(halEnvelope(Seq(
              invitation(arn, "Pending", "HMRC-MTD-IT", "personal", "ni","AB123456A", "ABERULMHCKKW3", "2017-12-18"),
              invitation(arn, "Pending", "HMRC-MTD-VAT", "business", "vrn", "101747696", "CZTW1KY6RTAAT", "2017-12-18")).mkString("[", ",", "]")))))

  def givenAllInvitationsRespondedStub(arn: Arn): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .withQueryParam("createdOnOrAfter", equalTo(LocalDate.now.minusDays(30).toString("yyyy-MM-dd")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(halEnvelope(Seq(
              invitation(arn, "Accepted", "HMRC-MTD-IT", "personal", "ni", "AB123456A", "foo4", "2017-12-18"),
              invitation(arn, "Rejected", "HMRC-MTD-VAT",  "business", "vrn", "101747696", "foo2", "2017-12-18"),
              invitation(arn, "Cancelled", "PERSONAL-INCOME-RECORD", "personal", "ni", "AB123456B", "fo11", "2017-12-18")
            ).mkString("[", ",", "]")))))

  def givenAllInvitationsPirStub(arn: Arn): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .withQueryParam("createdOnOrAfter", equalTo(LocalDate.now.minusDays(30).toString("yyyy-MM-dd")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(halEnvelope(Seq(
              invitation(arn, "Pending", "PERSONAL-INCOME-RECORD", "personal", "ni", "AB123456B", "foo1", "2017-12-18"),
              invitation(arn, "Cancelled", "PERSONAL-INCOME-RECORD", "personal", "ni", "AB123456B", "foo2", "2017-12-18"),
              invitation(arn, "Cancelled", "PERSONAL-INCOME-RECORD", "personal", "ni", "AB123456B", "foo3", "2017-12-18"),
              invitation(arn, "Cancelled", "PERSONAL-INCOME-RECORD", "personal", "ni", "AB123456B", "foo4", "2017-12-18")
            ).mkString("[", ",", "]")))))

  def halEnvelope(embedded: String): String =
    s"""{"_links": {
        "invitations": [
          {
            "href": "/agent-client-authorisation/agencies/TARN0000001/invitations/sent/AK77NLH3ETXM9"
          }
        ],
        "self": {
          "href": "/agent-client-authorisation/agencies/TARN0000001/invitations/sent"
        }
      },
      "_embedded": {
        "invitations": $embedded
      }
    }""".stripMargin

  def givenAllInvitationsEmptyStub(arn: Arn): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(halEnvelope("[]"))))

  val invitation = (
    arn: Arn,
    status: String,
    service: String,
    clientType: String,
    clientIdType: String,
    clientId: String,
    invitationId: String,
    expiryDate: String) => s"""
                              |{
                              |  "arn" : "${arn.value}",
                              |  "service" : "$service",
                              |  "clientType": "$clientType",
                              |  "clientId" : "$clientId",
                              |  "clientIdType" : "$clientIdType",
                              |  "suppliedClientId" : "$clientId",
                              |  "suppliedClientIdType" : "$clientIdType",
                              |  "status" : "$status",
                              |  "created" : "2017-10-31T23:22:50.971Z",
                              |  "lastUpdated" : "2018-09-11T21:02:00.000Z",
                              |  "expiryDate" : "$expiryDate",
                              |  "invitationId": "$invitationId",
                              |  "_links": {
                              |    	"self" : {
                              |			  "href" : "$wireMockBaseUrlAsString/agent-client-authorisation/agencies/${arn.value}/invitations/sent/$invitationId"
                              |		  }
                              |  }
                              |}""".stripMargin

  def givenInvitationNotFound(arn: Arn, invitationId: InvitationId): Unit =
    stubFor(
      get(urlEqualTo(s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"))
        .willReturn(aResponse()
          .withStatus(404)))

  def givenCancelAgentInvitationStub(arn: Arn, invitationId: InvitationId, status: Int) =
    stubFor(
      put(
        urlEqualTo(s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}/cancel"))
        .willReturn(aResponse()
          .withStatus(status)))

  def givenCancelAgentInvitationStubInvalid(arn: Arn, invitationId: InvitationId) =
    stubFor(
      put(
        urlEqualTo(s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}/cancel"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withBody(s"""
                         |{
                         |   "code":"INVALID_INVITATION_STATUS",
                         |   "message":"The inivtation has an invalid status to be cancelled"
                         |}
           """.stripMargin)))
}
