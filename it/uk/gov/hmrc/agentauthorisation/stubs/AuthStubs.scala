package uk.gov.hmrc.agentauthorisation.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentauthorisation.support.WireMockSupport
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys

trait AuthStubs {
  me: WireMockSupport =>

  case class Enrolment(serviceName: String, identifierName: String, identifierValue: String)

  def authorisedAsValidAgent[A](request: FakeRequest[A], arn: String) =
    authenticatedAgent(request, Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", arn))

  def authorisedAsValidClientITSA[A](request: FakeRequest[A], mtditid: String) =
    authenticatedClient(request, Enrolment("HMRC-MTD-IT", "MTDITID", mtditid))

  def authorisedAsValidClientAFI[A](request: FakeRequest[A], clientId: String) =
    authenticatedClient(request, Enrolment("HMRC-NI", "NINO", clientId))

  def authorisedAsValidClientVAT[A](request: FakeRequest[A], clientId: String) =
    authenticatedClient(request, Enrolment("HMRC-MTD-VAT", "VRN", clientId))

  def authenticatedClient[A](
    request: FakeRequest[A],
    enrolment: Enrolment,
    confidenceLevel: String = "200"): FakeRequest[A] = {
    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    { "identifiers":[], "state":"Activated", "enrolment": "${enrolment.serviceName}" },
         |    { "authProviders": ["GovernmentGateway"] },
         |    {"confidenceLevel":$confidenceLevel}
         |    ],
         |  "retrieve":["authorisedEnrolments"]
         |}
           """.stripMargin,
      s"""
         |{
         |  "authorisedEnrolments": [
         |    { "key":"${enrolment.serviceName}", "identifiers": [
         |      {"key":"${enrolment.identifierName}", "value": "${enrolment.identifierValue}"}
         |    ]}
         |]}
          """.stripMargin)
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def authenticatedAgent[A](request: FakeRequest[A], enrolment: Enrolment): FakeRequest[A] = {
    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    { "identifiers":[], "state":"Activated", "enrolment": "${enrolment.serviceName}" },
         |    { "authProviders": ["GovernmentGateway"] }
         |  ],
         |  "retrieve":["authorisedEnrolments"]
         |}
           """.stripMargin,
      s"""
         |{
         |"authorisedEnrolments": [
         |  { "key":"${enrolment.serviceName}", "identifiers": [
         |    {"key":"${enrolment.identifierName}", "value": "${enrolment.identifierValue}"}
         |  ]}
         |]}
          """.stripMargin)
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def givenUnauthorisedWith(mdtpDetail: String): Unit =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")))

  def givenAuthorisedFor(payload: String, responseBody: String): Unit =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)))

  def givenUnauthorisedForInsufficientEnrolments(): Unit =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")))

  def givenUnauthorisedForInsufficientConfidenceLevel(): Unit =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientConfidenceLevel\"")))

  def verifyAuthoriseAttempt(): Unit =
    verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))

  def verifyNoAuthoriseAttempt(): Unit =
    verify(0, postRequestedFor(urlEqualTo("/auth/authorise")))

}
