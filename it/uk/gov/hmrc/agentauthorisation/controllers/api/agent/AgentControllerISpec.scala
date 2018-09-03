package uk.gov.hmrc.agentauthorisation.controllers.api.agent

import akka.util.Timeout
import org.joda.time.LocalDate
import org.scalatest.time.Milliseconds
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.agentauthorisation._
import uk.gov.hmrc.agentauthorisation.models.{ PendingInvitation, RespondedInvitation, StoredInvitation }
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.SessionKeys
import play.api.libs.json.Json._
import play.api.mvc.Results._
import play.api.test.Helpers.contentAsJson

import scala.concurrent.duration.Duration

class AgentControllerISpec extends BaseISpec {

  lazy val controller: AgentController = app.injector.instanceOf[AgentController]

  val jsonBodyITSA: JsValue = Json.parse(
    s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}""")
  val jsonBodyVAT: JsValue = Json.parse(
    s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "$validVatRegDate"}""")

  val storedItsaInvitation = StoredInvitation(
    s"$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent/ABERULMHCKKW3",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    "2017-10-31T23:22:50.971Z",
    Arn("TARN0000001"),
    "MTD-IT",
    "Pending")

  val pendingItsaInvitation = PendingInvitation(
    s"$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent/ABERULMHCKKW3",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    Arn("TARN0000001"),
    "MTD-IT",
    "Pending",
    s"http://localhost:9448/invitations/${invitationIdITSA.value}")

  val respondedItsaInvitation = RespondedInvitation(
    s"$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent/ABERULMHCKKW3",
    "2017-10-31T23:22:50.971Z",
    "2017-10-31T23:22:50.971Z",
    Arn("TARN0000001"),
    "MTD-IT",
    "Accepted")

  val storedVatInvitation = StoredInvitation(
    s"$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent/CZTW1KY6RTAAT",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    "2017-10-31T23:22:50.971Z",
    Arn("TARN0000001"),
    "MTD-VAT",
    "Pending")

  val pendingVatInvitation = PendingInvitation(
    s"$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent/CZTW1KY6RTAAT",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    Arn("TARN0000001"),
    "MTD-VAT",
    "Pending",
    s"http://localhost:9448/invitations/${invitationIdVAT.value}")

  val respondedVatInvitation = RespondedInvitation(
    s"$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent/CZTW1KY6RTAAT",
    "2017-10-31T23:22:50.971Z",
    "2017-10-31T23:22:50.971Z",
    Arn("TARN0000001"),
    "MTD-VAT",
    "Accepted")

  "/agents/:arn/invitations" should {

    val request = FakeRequest("POST", s"/agents/${arn.value}/invitations")
    val createInvitation = controller.createInvitationApi(arn)

    "return 204 when invitation is successfully created for ITSA" in {
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      createInvitationStub(arn, validNino.value, invitationIdITSA, validNino.value, "ni", "HMRC-MTD-IT", "MTDITID", validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 204
      result.header.headers("Location") shouldBe routes.AgentController.getInvitationApi(arn, invitationIdITSA).url
    }

    "return 204 when invitation is successfully created for VAT" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
      createInvitationStub(arn, validVrn.value, invitationIdVAT, validVrn.value, "vrn", "HMRC-MTD-VAT", "VRN", validVatRegDate)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      status(result) shouldBe 204
      result.header.headers("Location") shouldBe (routes.AgentController.getInvitationApi(arn, invitationIdVAT).url)
    }

    "return 400 SERVICE_NOT_SUPPORTED when the service is not supported" in {
      val jsonBodyInvalidService = Json.parse(
        s"""{"service": ["foo"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe UnsupportedService
    }

    "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for ITSA" in {
      val jsonBodyInvalidService = Json.parse(
        s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "foo", "knownFact": "$validPostcode"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe InvalidItsaNino
    }

    "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for VAT" in {
      val jsonBodyInvalidService = Json.parse(
        s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "foo", "knownFact": "$validVatRegDate"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe InvalidVatVrn
    }

    "return 400 POSTCODE_FORMAT_INVALID when the postcode has an invalid format" in {
      val jsonBodyInvalidService = Json.parse(
        s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "foo"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe PostcodeFormatInvalid
    }

    "return 400 VAT_REG_DATE_FORMAT_INVALID when the VAT registration date has an invalid format" in {
      val jsonBodyInvalidService = Json.parse(
        s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "foo"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe VatRegDateFormatInvalid
    }

    "return 403 CLIENT_REGISTRATION_NOT_FOUND when the postcode returns nothing" in {
      givenNotEnrolledClientITSA(validNino, validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe ClientRegistrationNotFound
    }

    "return 403 CLIENT_REGISTRATION_NOT_FOUND when the VAT registration date returns nothing" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 404)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe ClientRegistrationNotFound
    }

    "return 403 POSTCODE_DOES_NOT_MATCH when the postcode and clientId do not match" in {
      givenNonMatchingClientIdAndPostcode(validNino, validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe PostcodeDoesNotMatch
    }

    "return 403 VAT_REG_DATE_DOES_NOT_MATCH when the VAT registration date and clientId do not match" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 403)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe VatRegDateDoesNotMatch
    }

    "return 403 NOT_AN_AGENT when the logged in user is not have an HMRC-AS-AGENT enrolment" in {
      givenUnauthorisedForInsufficientEnrolments()
      val result = createInvitation(request.withJsonBody(jsonBodyITSA))

      status(result) shouldBe 401
      await(result) shouldBe InvalidCredentials
    }

    "return 403 NOT_PERMISSION_ON_AGENCY when the logged in user does not have an HMRC-AS-AGENT enrolment" in {
      createInvitationStub(arn, validNino.value, invitationIdITSA, validNino.value, "ni", "HMRC-MTD-IT", "MTDITID", validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn2.value))

      status(result) shouldBe 403
      await(result) shouldBe NoPermissionOnAgency
    }

    "return a future failed when the invitation creation failed for ITSA" in {
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      failedCreateInvitation(arn)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      an[Exception] shouldBe thrownBy {
        await(result)
      }
    }

    "return a future failed when the invitation creation failed for VAT" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
      failedCreateInvitation(arn)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      an[Exception] shouldBe thrownBy {
        await(result)
      }
    }
  }

  "/agents/:arn/invitations/:invitationId" when {

    "requesting an ITSA invitation" should {

      val getInvitationItsaApi = controller.getInvitationApi(arn, invitationIdITSA)
      val requestITSA = FakeRequest("GET", s"/agents/${arn.value}/invitations/${invitationIdITSA.value}")

      implicit val timeout: Timeout = Timeout(Duration.Zero)

      "return 200 and a json body of a pending invitation" in {
        givenGetITSAInvitationStub(arn, "Pending")
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        contentAsJson(result) shouldBe toJson(pendingItsaInvitation).as[JsObject]
      }

      "return 200 and a json body of a responded invitation" in {
        givenGetITSAInvitationStub(arn, "Accepted")
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))

        status(result) shouldBe 200
        contentAsJson(result) shouldBe toJson(respondedItsaInvitation).as[JsObject]
      }

      "return 401 for Invalid Credentials" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = getInvitationItsaApi(requestITSA)

        status(result) shouldBe 401
        await(result) shouldBe InvalidCredentials
      }

      "return 403 for Not an Agent" in {
        givenGetITSAInvitationStub(arn, "Pending")
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
             |    { "identifiers":[], "state":"Activated", "enrolment": "HMRC-AS-AGENT" },
             |    { "authProviders": ["GovernmentGateway"] }
             |  ],
             |  "retrieve":["affinityGroup","allEnrolments"]
             |}
           """.stripMargin,
          s"""
             |{
             |"affinityGroup":"Individual",
             |"allEnrolments": [
             |  { "key":"HMRC-MTD-IT", "identifiers": [
             |    {"key":"MTDITID", "value": "${mtdItId.value}"}
             |  ]}
             |]}
          """.stripMargin)

        val result = getInvitationItsaApi(requestITSA.withSession(SessionKeys.authToken -> "Bearer XYZ"))

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgent
      }

      "return 403 for Agent Not Subscribed" in {
        givenGetITSAInvitationStub(arn, "Pending")
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
             |    { "identifiers":[], "state":"Activated", "enrolment": "HMRC-AS-AGENT" },
             |    { "authProviders": ["GovernmentGateway"] }
             |  ],
             |  "retrieve":["affinityGroup","allEnrolments"]
             |}
           """.stripMargin,
          s"""
             |{
             |"affinityGroup":"Agent",
             |"allEnrolments": [
             |  { "key":"IR-SA-AGENT", "identifiers": [
             |    {"key":"IRAgentReference", "value": "someIRAR"}
             |  ]}
             |]}
          """.stripMargin)

        val result = getInvitationItsaApi(requestITSA.withSession(SessionKeys.authToken -> "Bearer XYZ"))

        status(result) shouldBe 403
        await(result) shouldBe AgentNotSubscribed
      }

      "return 403 for No Permission On Agency" in {
        givenGetITSAInvitationStub(arn, "Pending")
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn2.value))
        status(result) shouldBe 403
        await(result) shouldBe NoPermissionOnAgency
      }

      "return 404 for Invitation Not Found" in {
        givenInvitationNotFound(arn, invitationIdITSA)
        val result = getInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))
        status(result) shouldBe 404
        await(result) shouldBe InvitationNotFound
      }
    }

    "requesting an VAT invitation" should {
      val getInvitationVatApi = controller.getInvitationApi(arn, invitationIdVAT)
      val requestVAT = FakeRequest("GET", s"/agents/${arn.value}/invitations/${invitationIdVAT.value}")

      implicit val timeout: Timeout = Timeout(Duration.Zero)

      "return 200 and a json body of invitation" in {
        givenGetVATInvitationStub(arn, "Pending")
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 200
        contentAsJson(result) shouldBe toJson(pendingVatInvitation).as[JsObject]
      }

      "return 200 and a json body of a responded invitation" in {
        givenGetVATInvitationStub(arn, "Accepted")
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))

        status(result) shouldBe 200
        contentAsJson(result) shouldBe toJson(respondedVatInvitation).as[JsObject]
      }

      "return 401 for Invalid Credentials" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = getInvitationVatApi(requestVAT)

        status(result) shouldBe 401
        await(result) shouldBe InvalidCredentials
      }

      "return 403 for Not an Agent" in {
        givenGetVATInvitationStub(arn, "Pending")
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
             |    { "identifiers":[], "state":"Activated", "enrolment": "HMRC-AS-AGENT" },
             |    { "authProviders": ["GovernmentGateway"] }
             |  ],
             |  "retrieve":["affinityGroup","allEnrolments"]
             |}
           """.stripMargin,
          s"""
             |{
             |"affinityGroup":"Individual",
             |"allEnrolments": [
             |  { "key":"HMRC-MTD-IT", "identifiers": [
             |    {"key":"MTDITID", "value": "${mtdItId.value}"}
             |  ]}
             |]}
          """.stripMargin)

        val result = getInvitationVatApi(requestVAT.withSession(SessionKeys.authToken -> "Bearer XYZ"))

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgent
      }

      "return 403 for Agent Not Subscribed" in {
        givenGetVATInvitationStub(arn, "Pending")
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
             |    { "identifiers":[], "state":"Activated", "enrolment": "HMRC-AS-AGENT" },
             |    { "authProviders": ["GovernmentGateway"] }
             |  ],
             |  "retrieve":["affinityGroup","allEnrolments"]
             |}
           """.stripMargin,
          s"""
             |{
             |"affinityGroup":"Agent",
             |"allEnrolments": [
             |  { "key":"IR-SA-AGENT", "identifiers": [
             |    {"key":"IRAgentReference", "value": "someIRAR"}
             |  ]}
             |]}
          """.stripMargin)

        val result = getInvitationVatApi(requestVAT.withSession(SessionKeys.authToken -> "Bearer XYZ"))

        status(result) shouldBe 403
        await(result) shouldBe AgentNotSubscribed
      }

      "return 403 for No Permission On Agency" in {
        givenGetVATInvitationStub(arn, "Pending")
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn2.value))
        status(result) shouldBe 403
        await(result) shouldBe NoPermissionOnAgency
      }

      "return 404 for Invitation Not Found" in {
        givenInvitationNotFound(arn, invitationIdVAT)
        val result = getInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))
        status(result) shouldBe 404
        await(result) shouldBe InvitationNotFound
      }

    }

  }
}
