package uk.gov.hmrc.agentauthorisation.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.LocalDate
import uk.gov.hmrc.agentauthorisation.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.{ Arn, InvitationId, Vrn }
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
    service: String,
    serviceIdentifier: String,
    knownFact: String): Unit =
    stubFor(
      post(urlEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .withRequestBody(
          equalToJson(s"""
                         |{
                         |   "service": "$service",
                         |   "clientIdType": "$suppliedClientType",
                         |   "clientId":"$suppliedClientId",
                         |   "knownFact":"$knownFact"
                         |}""".stripMargin))
        .willReturn(
          aResponse()
            .withStatus(201)
            .withHeader(
              "location",
              s"$wireMockBaseUrlAsString/agent-client-authorisation/clients/$serviceIdentifier/${
                encodePathSegment(
                  clientId)
              }/invitations/received/${invitationId.value}")))

  def failedCreateInvitation(arn: Arn): Unit =
    stubFor(
      post(urlEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .willReturn(aResponse()
          .withStatus(400)))

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
        urlEqualTo(
          s"/agent-client-authorisation/known-facts/individuals/nino/$ninoEncoded/sa/postcode/$postEncoded")))
  }

  def verifyNoCheckVatRegisteredClientStubAttempt(): Unit =
    verify(
      0,
      getRequestedFor(
        urlPathMatching("/agent-client-authorisation/known-facts/organisations/.*/registration-date/.*")))

  def givenGetITSAInvitationStub(arn: Arn, status: String): Unit = givenGetAgentInvitationStub(
    arn,
    "ni",
    validNino.value,
    invitationIdITSA,
    serviceITSA,
    status)

  def givenGetVATInvitationStub(arn: Arn, status: String): Unit = givenGetAgentInvitationStub(
    arn,
    "ni",
    validVrn.value,
    invitationIdVAT,
    serviceVAT,
    status)

  def givenGetAgentInvitationStub(
    arn: Arn,
    clientIdType: String,
    clientId: String,
    invitationId: InvitationId,
    service: String,
    status: String): Unit =
    stubFor(
      get(urlEqualTo(
        s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "arn" : "${arn.value}",
                         |  "service" : "$service",
                         |  "clientId" : "$clientId",
                         |  "clientIdType" : "$clientIdType",
                         |  "suppliedClientId" : "$clientId",
                         |  "suppliedClientIdType" : "$clientIdType",
                         |  "status" : "$status",
                         |  "created" : "2017-10-31T23:22:50.971Z",
                         |  "lastUpdated" : "2017-10-31T23:22:50.971Z",
                         |  "expiryDate" : "2017-12-18",
                         |  "_links": {
                         |    	"self" : {
                         |			  "href" : "$wireMockBaseUrlAsString/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"
                         |		  }
                         |  }
                         |}""".stripMargin)))

  def givenInvitationNotFound(arn: Arn, invitationId: InvitationId): Unit = {
    stubFor(
      get(urlEqualTo(s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"))
        .willReturn(
          aResponse()
            .withStatus(404)))
  }

  def givenCancelAgentInvitationStub(arn: Arn, invitationId: InvitationId, status: Int) =
    stubFor(put(urlEqualTo(s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}/cancel"))
      .willReturn(
        aResponse()
          .withStatus(status)))

  def givenCancelAgentInvitationStubInvalid(arn: Arn, invitationId: InvitationId) =
    stubFor(put(urlEqualTo(s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}/cancel"))
      .willReturn(
        aResponse()
          .withStatus(401).withBody(s"""
                                          |{
                                          |   "code":"INVALID_INVITATION_STATUS",
                                          |   "message":"The inivtation has an invalid status to be cancelled"
                                          |}
           """.stripMargin)))
}
