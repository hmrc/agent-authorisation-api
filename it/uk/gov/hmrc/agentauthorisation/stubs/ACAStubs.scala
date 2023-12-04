package uk.gov.hmrc.agentauthorisation.stubs

import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentauthorisation.UriPathEncoding.encodePathSegment
import uk.gov.hmrc.agentauthorisation.support.{TestIdentifiers, WireMockSupport}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, Vrn}
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}

trait ACAStubs {
  me: WireMockSupport with TestIdentifiers =>

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

  def checkClientIdAndVatRegDate(vrn: Vrn, date: LocalDate, responseStatus: Int, clientInsolvent: Boolean = false) = {
    val responseCode = if(responseStatus == 403) if(clientInsolvent) "VAT_RECORD_CLIENT_INSOLVENT_TRUE" else "VAT_REGISTRATION_DATE_DOES_NOT_MATCH" else ""
    stubFor(
      get(urlEqualTo(
        s"/agent-client-authorisation/known-facts/organisations/vat/${vrn.value}/registration-date/${date.toString}"))
        .willReturn(aResponse()
          .withStatus(responseStatus)
          .withBody(
              s"""{
                 |"code": "$responseCode"
                 |}""".stripMargin
            )))
  }

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
                         |  "clientActionUrl": "someInvitationUrl/invitations/$clientType/12345678/agent-1",
                         |  "_links": {
                         |    	"self" : {
                         |			  "href" : "$wireMockBaseUrlAsString/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"
                         |		  }
                         |  }
                         |}""".stripMargin)))

  def givenGetAgentInvitationStubReturns(arn: Arn, invitationId: InvitationId, status: Int) =
    stubFor(
      get(urlEqualTo(s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"))
        .willReturn(aResponse()
          .withStatus(status)))

  //TODO Update Test when to modify for specific GET ALL for API

  def givenInvitationsServiceReturns(arn: Arn, invitations: Seq[String]): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .withQueryParam("service", equalTo("HMRC-MTD-IT,HMRC-MTD-VAT"))
        .withQueryParam("createdOnOrAfter", equalTo(LocalDate.now.minusDays(30).toString))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(halEnvelope(invitations.mkString("[", ",", "]")))))

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
                              |  "clientActionUrl": "someInvitationUrl/invitations/$clientType/12345678/agent-1",
                              |  "_links": {
                              |    	"self" : {
                              |			  "href" : "$wireMockBaseUrlAsString/agent-client-authorisation/agencies/${arn.value}/invitations/sent/$invitationId"
                              |		  }
                              |  }
                              |}""".stripMargin

  val itsa: Arn => String = (arn: Arn) =>
    invitation(arn, "Pending", "HMRC-MTD-IT", "personal", "ni", "AB123456A", "ABERULMHCKKW3", "2017-12-18")
  val vat: Arn => String = (arn: Arn) =>
    invitation(arn, "Pending", "HMRC-MTD-VAT", "business", "vrn", "101747696", "CZTW1KY6RTAAT", "2017-12-18")
  val irv: Arn => String = (arn: Arn) =>
    invitation(arn, "Cancelled", "PERSONAL-INCOME-RECORD", "personal", "ni", "AB123456B", "fo11", "2017-12-18")
  val ters: Arn => String = (arn: Arn) =>
    invitation(arn, "Accepted", "HMRC-TERS-ORG", "business", "utr", "AB123456B", "foo1", "2017-12-18")

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

  def givenOnlyPendingInvitationsExistForClient(arn: Arn, clientId: TaxIdentifier, service: String): StubMapping = {
    val body = service match {
      case "HMRC-MTD-IT" =>
        invitation(arn, "Pending", "HMRC-MTD-IT", "personal", "ni", clientId.value, "foo", "2020-10-10")
      case "HMRC-MTD-VAT" =>
        invitation(arn, "Pending", "HMRC-MTD-VAT", "personal", "vrn", clientId.value, "bar", "2020-10-10")
    }

    stubFor(
      get(
        urlPathEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent")
      ).withQueryParam("clientId", equalTo(clientId.value))
        .withQueryParam("service", equalTo(service))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""{"_links": {
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
        "invitations": [$body]
      }
    }""".stripMargin)
        )
    )
  }

  def givenOnlyActiveInvitationsExistForClient(arn: Arn, clientId: TaxIdentifier, service: String): StubMapping = {
    val body = service match {
      case "HMRC-MTD-IT" =>
        invitation(arn, "Active", "HMRC-MTD-IT", "personal", "ni", clientId.value, "foo", "2020-10-10")
      case "HMRC-MTD-VAT" =>
        invitation(arn, "Active", "HMRC-MTD-VAT", "personal", "vrn", clientId.value, "bar", "2020-10-10")
    }

    stubFor(
      get(
        urlPathEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent")
      ).withQueryParam("clientId", equalTo(clientId.value))
        .withQueryParam("service", equalTo(service))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""{"_links": {
                         |        "invitations": [
                         |          {
                         |            "href": "/agent-client-authorisation/agencies/TARN0000001/invitations/sent/AK77NLH3ETXM9"
                         |          }
                         |        ],
                         |        "self": {
                         |          "href": "/agent-client-authorisation/agencies/TARN0000001/invitations/sent"
                         |        }
                         |      },
                         |      "_embedded": {
                         |        "invitations": [$body]
                         |      }
                         |    }""".stripMargin)))
  }

  def givenNoInvitationsExistForClient(arn: Arn, clientId: TaxIdentifier, service: String): StubMapping = {
    stubFor(
      get(
        urlPathEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent")
      ).withQueryParam("clientId", equalTo(clientId.value))
        .withQueryParam("service", equalTo(service))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""{"_links": {
                         |        "invitations": [
                         |          {
                         |            "href": "/agent-client-authorisation/agencies/TARN0000001/invitations/sent/AK77NLH3ETXM9"
                         |          }
                         |        ],
                         |        "self": {
                         |          "href": "/agent-client-authorisation/agencies/TARN0000001/invitations/sent"
                         |        }
                         |      },
                         |      "_embedded": {
                         |        "invitations": []
                         |      }
                         |    }""".stripMargin)))
  }
}
