package uk.gov.hmrc.agentauthorisation.controllers.api.agent

import akka.util.Timeout
import org.joda.time.LocalDate
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.agentauthorisation._
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{ Arn, InvitationId }
import uk.gov.hmrc.http.SessionKeys
import play.api.libs.json.Json._
import play.api.test.Helpers.contentAsJson
import uk.gov.hmrc.agentauthorisation.audit.AgentAuthorisationEvent

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
    s"/agents/TARN0000001/invitations/ABERULMHCKKW3",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    Arn("TARN0000001"),
    List("MTD-IT"),
    "Pending",
    s"http://localhost:9448/invitations/${invitationIdITSA.value}")

  val respondedItsaInvitation = RespondedInvitation(
    s"/agents/TARN0000001/invitations/ABERULMHCKKW3",
    "2017-10-31T23:22:50.971Z",
    "2017-10-31T23:22:50.971Z",
    Arn("TARN0000001"),
    List("MTD-IT"),
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
    s"/agents/TARN0000001/invitations/CZTW1KY6RTAAT",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    Arn("TARN0000001"),
    List("MTD-VAT"),
    "Pending",
    s"http://localhost:9448/invitations/${invitationIdVAT.value}")

  val respondedVatInvitation = RespondedInvitation(
    s"/agents/TARN0000001/invitations/CZTW1KY6RTAAT",
    "2017-10-31T23:22:50.971Z",
    "2017-10-31T23:22:50.971Z",
    Arn("TARN0000001"),
    List("MTD-VAT"),
    "Accepted")

  val gettingPendingInvitations = Seq(
    PendingOrRespondedInvitation(
      s"/agents/${arn.value}/invitations/foo1",
      "2017-10-31T23:22:50.971Z",
      arn,
      List("MTD-IT"),
      "Pending",
      Some("2017-12-18T00:00:00.000"),
      Some("http://localhost:9448/invitations/foo1"),
      None),
    PendingOrRespondedInvitation(
      s"/agents/${arn.value}/invitations/foo2",
      "2017-10-31T23:22:50.971Z",
      arn,
      List("MTD-VAT"),
      "Pending",
      Some("2017-12-18T00:00:00.000"),
      Some("http://localhost:9448/invitations/foo2"),
      None))

  val gettingRespondedInvitations = Seq(
    PendingOrRespondedInvitation(
      s"/agents/${arn.value}/invitations/foo4",
      "2017-10-31T23:22:50.971Z",
      arn,
      List("MTD-IT"),
      "Accepted",
      None,
      None,
      Some("2018-09-11T21:02:00.000Z")),
    PendingOrRespondedInvitation(
      s"/agents/${arn.value}/invitations/foo2",
      "2017-10-31T23:22:50.971Z",
      arn,
      List("MTD-VAT"),
      "Rejected",
      None,
      None,
      Some("2018-09-11T21:02:00.000Z")))

  "/agents/:arn/invitations" should {

    val request = FakeRequest("POST", s"/agents/${arn.value}/invitations")
    val createInvitation = controller.createInvitationApi(arn)

    "return 204 when invitation is successfully created for ITSA" in {
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      createInvitationStub(
        arn,
        validNino.value,
        invitationIdITSA,
        validNino.value,
        "ni",
        "HMRC-MTD-IT",
        "MTDITID",
        validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 204
      result.header.headers("Location") shouldBe routes.AgentController.getInvitationApi(arn, invitationIdITSA).url
      verifyAgentClientInvitationSubmittedEvent(arn.value, validNino.value, "ni", "Success", "HMRC-MTD-IT", None)
    }

    "return 204 when invitation is successfully created for VAT" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
      createInvitationStub(
        arn,
        validVrn.value,
        invitationIdVAT,
        validVrn.value,
        "vrn",
        "HMRC-MTD-VAT",
        "VRN",
        validVatRegDate)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      status(result) shouldBe 204
      result.header.headers("Location") shouldBe routes.AgentController.getInvitationApi(arn, invitationIdVAT).url
      verifyAgentClientInvitationSubmittedEvent(arn.value, validVrn.value, "vrn", "Success", "HMRC-MTD-VAT", None)
    }

    "return 400 SERVICE_NOT_SUPPORTED when the service is not supported" in {
      val jsonBodyInvalidService = Json.parse(
        s"""{"service": ["foo"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe UnsupportedService
      verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
    }

    "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for ITSA" in {
      val jsonBodyInvalidClientId = Json.parse(
        s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "foo", "knownFact": "$validPostcode"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe ClientIdInvalidFormat
      verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
    }

    "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for VAT" in {
      val jsonBodyInvalidClientId = Json.parse(
        s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "foo", "knownFact": "$validVatRegDate"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe ClientIdInvalidFormat
      verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
    }

    "return 400 POSTCODE_FORMAT_INVALID when the postcode has an invalid format" in {
      val jsonBodyInvalidPostcode = Json.parse(
        s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "foo"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidPostcode), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe PostcodeFormatInvalid
      verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
    }

    "return 400 VAT_REG_DATE_FORMAT_INVALID when the VAT registration date has an invalid format" in {
      val jsonBodyInvalidVatRegDate = Json.parse(
        s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "foo"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidVatRegDate), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe VatRegDateFormatInvalid
      verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
    }

    "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for ITSA" in {
      val jsonBodyClientIdNotMatchService = Json.parse(
        s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validVrn.value}", "knownFact": "foo"}""")

      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe ClientIdDoesNotMatchService
      verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
    }

    "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for VAT" in {
      val jsonBodyClientIdNotMatchService = Json.parse(
        s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "${validNino.value}", "knownFact": "foo"}""")

      val result =
        createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe ClientIdDoesNotMatchService
      verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
    }

    "return 403 CLIENT_REGISTRATION_NOT_FOUND when the postcode returns nothing" in {
      givenNotEnrolledClientITSA(validNino, validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe ClientRegistrationNotFound
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validNino.value,
        "ni",
        "Fail",
        "HMRC-MTD-IT",
        Some("CLIENT_REGISTRATION_NOT_FOUND"))
    }

    "return 403 CLIENT_REGISTRATION_NOT_FOUND when the VAT registration date returns nothing" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 404)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe ClientRegistrationNotFound
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validVrn.value,
        "vrn",
        "Fail",
        "HMRC-MTD-VAT",
        Some("CLIENT_REGISTRATION_NOT_FOUND"))
    }

    "return 403 POSTCODE_DOES_NOT_MATCH when the postcode and clientId do not match" in {
      givenNonMatchingClientIdAndPostcode(validNino, validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe PostcodeDoesNotMatch
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validNino.value,
        "ni",
        "Fail",
        "HMRC-MTD-IT",
        Some("POSTCODE_DOES_NOT_MATCH"))
    }

    "return 403 VAT_REG_DATE_DOES_NOT_MATCH when the VAT registration date and clientId do not match" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 403)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe VatRegDateDoesNotMatch
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validVrn.value,
        "vrn",
        "Fail",
        "HMRC-MTD-VAT",
        Some("VAT_REG_DATE_DOES_NOT_MATCH"))
    }

    "return 403 NOT_AN_AGENT when the logged in user is not have an HMRC-AS-AGENT enrolment" in {
      givenUnauthorisedForInsufficientEnrolments()
      val result = createInvitation(request.withJsonBody(jsonBodyITSA))

      status(result) shouldBe 403
      await(result) shouldBe NotAnAgent
      verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
    }

    "return 403 NOT_PERMISSION_ON_AGENCY when the logged in user does not have an HMRC-AS-AGENT enrolment" in {
      createInvitationStub(
        arn,
        validNino.value,
        invitationIdITSA,
        validNino.value,
        "ni",
        "HMRC-MTD-IT",
        "MTDITID",
        validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn2.value))

      status(result) shouldBe 403
      await(result) shouldBe NoPermissionOnAgency
      verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
    }

    "return a future failed when the invitation creation failed for ITSA" in {
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      failedCreateInvitation(arn)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      an[Exception] shouldBe thrownBy {
        await(result)
      }
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validNino.value,
        "ni",
        "Fail",
        "HMRC-MTD-IT",
        Some(
          s"POST of '$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent' returned 400 (Bad Request). Response body ''"))

    }

    "return a future failed when the invitation creation failed for VAT" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
      failedCreateInvitation(arn)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

      an[Exception] shouldBe thrownBy {
        await(result)
      }
      verifyAgentClientInvitationSubmittedEvent(
        arn.value,
        validVrn.value,
        "vrn",
        "Fail",
        "HMRC-MTD-VAT",
        Some(
          s"POST of '$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent' returned 400 (Bad Request). Response body ''"))
    }
  }

  "GET /agents/:arn/invitations/:invitationId" when {

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

      "return 403 for Not An Agent" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = getInvitationItsaApi(requestITSA)

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgent
      }

      "return 403 for Not an Agent" in {
        givenGetITSAInvitationStub(arn, "Pending")
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
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

      "return 403 for Not An Agent" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = getInvitationVatApi(requestVAT)

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgent
      }

      "return 403 for Not an Agent" in {
        givenGetVATInvitationStub(arn, "Pending")
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
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

  "DELETE /agents/:arn/invitations/:invitationId" when {
    "cancelling an ITSA invitation" should {

      val cancelInvitationItsaApi = controller.cancelInvitationApi(arn, invitationIdITSA)
      val requestITSA = FakeRequest("DELETE", s"/agents/${arn.value}/invitations/${invitationIdITSA.value}/cancel")

      "return 204 for a successful cancellation" in {
        givenCancelAgentInvitationStub(arn, invitationIdITSA, 204)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))
        status(result) shouldBe 204
        verifyAgentClientInvitationCancelledEvent(arn.value, invitationIdITSA)
      }

      "return 403 INVALID_INVITATION_STATUS when the status of the invitation is not Pending" in {
        givenCancelAgentInvitationStubInvalid(arn, invitationIdITSA)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn.value))
        await(result) shouldBe InvalidInvitationStatus
        verifyAgentClientInvitationCancelledEvent(arn.value, invitationIdITSA, Some("INVALID_INVITATION_STATUS"))
      }

      "return 403 NOT_AN_AGENT when the logged in user does not have an affinity group of Agent" in {
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
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
        val result = cancelInvitationItsaApi(requestITSA)

        await(result) shouldBe NotAnAgent
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisedCancelledViaApi)

      }

      "return 403 AGENT_NOT_SUBSCRIBED when the logged in user does not have HMRC_AS_AGENT enrolment" in {
        givenAuthorisedFor(
          s"""
             |{
             |  "authorise": [
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
        val result = cancelInvitationItsaApi(requestITSA)

        await(result) shouldBe AgentNotSubscribed
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisedCancelledViaApi)
      }

      "return 403 NO_PERMISSION_ON_AGENCY when the arn given does not match the logged in user" in {
        givenCancelAgentInvitationStub(arn, invitationIdITSA, 204)
        val result = cancelInvitationItsaApi(authorisedAsValidAgent(requestITSA, arn2.value))
        await(result) shouldBe NoPermissionOnAgency
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisedCancelledViaApi)
      }
    }

    "cancelling a VAT invitation" should {

      val cancelInvitationVatApi = controller.cancelInvitationApi(arn, invitationIdVAT)
      val requestVAT = FakeRequest("DELETE", s"/agents/${arn.value}/invitations/${invitationIdITSA.value}/cancel")

      "return 204 for a successful cancellation" in {
        givenCancelAgentInvitationStub(arn, invitationIdVAT, 204)
        val result = cancelInvitationVatApi(authorisedAsValidAgent(requestVAT, arn.value))
        status(result) shouldBe 204
        verifyAgentClientInvitationCancelledEvent(arn.value, invitationIdVAT)
      }
    }
  }

  "POST /agents/:arn/relationships" when {

    "getting the status of an ITSA relationship" should {
      val checkRelationshipApi = controller.checkRelationshipApi(arn)
      val request = FakeRequest("POST", s"/agents/$arn/relationships")

      "return 204 when the relationship is active for ITSA" in {
        givenMtdItIdIsKnownFor(validNino, mtdItId)
        getStatusRelationshipItsa(arn.value, mtdItId, 200)
        givenMatchingClientIdAndPostcode(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))
        status(result) shouldBe 204
      }

      "return 404 when the Nino cannot be converted to MtdItId" in {
        givenMtdItIdIsUnKnownFor(validNino)
        getStatusRelationshipItsa(arn.value, mtdItId, 200)
        givenMatchingClientIdAndPostcode(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))
        status(result) shouldBe 404
      }

      "return 204 when the relationship is active for VAT" in {
        getStatusRelationshipVat(arn.value, validVrn, 200)
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))
        status(result) shouldBe 204
      }

      "return 404 when the relationship is not found for ITSA" in {
        givenMtdItIdIsKnownFor(validNino, mtdItId)
        getStatusRelationshipItsa(arn.value, mtdItId, 404)
        givenMatchingClientIdAndPostcode(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))
        status(result) shouldBe 404
      }

      "return 404 when the relationship is not found for VAT" in {
        getStatusRelationshipVat(arn.value, validVrn, 404)
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))
        status(result) shouldBe 404
      }

      "return 400 SERVICE_NOT_SUPPORTED when the service is not supported" in {
        val jsonBodyInvalidService = Json.parse(
          s"""{"service": ["foo"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}""")

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe UnsupportedService
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
      }

      "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for ITSA" in {
        val jsonBodyInvalidClientId = Json.parse(
          s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "foo", "knownFact": "$validPostcode"}""")

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdInvalidFormat
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)

      }

      "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format for VAT" in {
        val jsonBodyInvalidClientId = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "foo", "knownFact": "$validVatRegDate"}""")

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidClientId), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdInvalidFormat
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
      }

      "return 400 POSTCODE_FORMAT_INVALID when the postcode has an invalid format" in {
        val jsonBodyInvalidPostcode = Json.parse(
          s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "foo"}""")

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidPostcode), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe PostcodeFormatInvalid
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)

      }

      "return 400 VAT_REG_DATE_FORMAT_INVALID when the VAT registration date has an invalid format" in {
        val jsonBodyInvalidVatRegDate = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "foo"}""")

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidVatRegDate), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe VatRegDateFormatInvalid
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)

      }

      "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for ITSA" in {
        val jsonBodyClientIdNotMatchService = Json.parse(
          s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validVrn.value}", "knownFact": "foo"}""")

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdDoesNotMatchService
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)

      }

      "return 400 CLIENT_ID_DOES_NOT_MATCH_SERVICE when the clientId is wrong for the service for VAT" in {
        val jsonBodyClientIdNotMatchService = Json.parse(
          s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "${validNino.value}", "knownFact": "foo"}""")

        val result =
          checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyClientIdNotMatchService), arn.value))

        status(result) shouldBe 400
        await(result) shouldBe ClientIdDoesNotMatchService
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
      }

      "return 403 CLIENT_REGISTRATION_NOT_FOUND when the postcode returns nothing" in {
        givenMtdItIdIsUnKnownFor(validNino)
        givenNotEnrolledClientITSA(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe ClientRegistrationNotFound
        verifyAgentCheckRelationshipStatusEvent(
          arn.value,
          validNino.value,
          "ni",
          "Fail",
          "HMRC-MTD-IT",
          Some("CLIENT_REGISTRATION_NOT_FOUND"))
      }

      "return 403 CLIENT_REGISTRATION_NOT_FOUND when the VAT registration date returns nothing" in {
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 404)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe ClientRegistrationNotFound
        verifyAgentCheckRelationshipStatusEvent(
          arn.value,
          validVrn.value,
          "vrn",
          "Fail",
          "HMRC-MTD-VAT",
          Some("CLIENT_REGISTRATION_NOT_FOUND"))
      }

      "return 403 POSTCODE_DOES_NOT_MATCH when the postcode and clientId do not match" in {
        givenMtdItIdIsUnKnownFor(validNino)
        givenNonMatchingClientIdAndPostcode(validNino, validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe PostcodeDoesNotMatch
        verifyAgentCheckRelationshipStatusEvent(
          arn.value,
          validNino.value,
          "ni",
          "Fail",
          "HMRC-MTD-IT",
          Some("POSTCODE_DOES_NOT_MATCH"))
      }

      "return 403 VAT_REG_DATE_DOES_NOT_MATCH when the VAT registration date and clientId do not match" in {
        checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 403)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyVAT), arn.value))

        status(result) shouldBe 403
        await(result) shouldBe VatRegDateDoesNotMatch
        verifyAgentCheckRelationshipStatusEvent(
          arn.value,
          validVrn.value,
          "vrn",
          "Fail",
          "HMRC-MTD-VAT",
          Some("VAT_REG_DATE_DOES_NOT_MATCH"))
      }

      "return 403 NOT_AN_AGENT when the logged in user is not have an HMRC-AS-AGENT enrolment" in {
        givenUnauthorisedForInsufficientEnrolments()
        val result = checkRelationshipApi(request.withJsonBody(jsonBodyITSA))

        status(result) shouldBe 403
        await(result) shouldBe NotAnAgent
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)

      }

      "return 403 NOT_PERMISSION_ON_AGENCY when the logged in user does not have an HMRC-AS-AGENT enrolment" in {
        createInvitationStub(
          arn,
          validNino.value,
          invitationIdITSA,
          validNino.value,
          "ni",
          "HMRC-MTD-IT",
          "MTDITID",
          validPostcode)
        val result = checkRelationshipApi(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn2.value))

        status(result) shouldBe 403
        await(result) shouldBe NoPermissionOnAgency
        verifyAuditRequestNotSent(AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi)
      }
    }

    "GET /agents/:arn/invitations/" when {

      "requesting a sequence of ITSA and VAT invitations" should {

        val getInvitations = controller.getInvitationsApi(arn)
        val request = FakeRequest("GET", s"/agents/${arn.value}/invitations")

        implicit val timeout: Timeout = Timeout(Duration.Zero)

        "return 200 and a json body of a pending invitation filtering out PIR relationships" in {
          givenAllInvitationsPendingStub(arn)
          val result = getInvitations(authorisedAsValidAgent(request, arn.value))

          status(result) shouldBe 200
          contentAsJson(result) shouldBe toJson(gettingPendingInvitations)
        }

        "return 200 and a json body of a responded invitation filtering out PIR relationships" in {
          givenAllInvitationsRespondedStub(arn)
          val result = getInvitations(authorisedAsValidAgent(request, arn.value))

          status(result) shouldBe 200
          contentAsJson(result) shouldBe toJson(gettingRespondedInvitations)
        }

        "return 204 if there are no relationships for the agent" in {
          givenAllInvitationsEmptyStub(arn)
          val result = getInvitations(authorisedAsValidAgent(request, arn.value))

          status(result) shouldBe 204
        }

        "return 204 if there are only PIR relationships for the agent" in {
          givenAllInvitationsPirStub(arn)
          val result = getInvitations(authorisedAsValidAgent(request, arn.value))

          status(result) shouldBe 204
        }
      }
    }
  }

  def verifyAgentClientInvitationSubmittedEvent(
    arn: String,
    clientId: String,
    clientIdType: String,
    result: String,
    service: String,
    failure: Option[String] = None): Unit =
    verifyAuditRequestSent(
      1,
      AgentAuthorisationEvent.AgentAuthorisationCreatedViaApi,
      detail = Map(
        "factCheck" -> result,
        "agentReferenceNumber" -> arn,
        "clientIdType" -> clientIdType,
        "clientId" -> clientId,
        "service" -> service)
        .filter(_._2.nonEmpty) ++ failure.map(e => Seq("failureDescription" -> e)).getOrElse(Seq.empty),
      tags = Map("transactionName" -> "Agent created invitation through third party software"))

  def verifyAgentClientInvitationCancelledEvent(
    arn: String,
    invitationId: InvitationId,
    failure: Option[String] = None): Unit =
    verifyAuditRequestSent(
      1,
      AgentAuthorisationEvent.AgentAuthorisedCancelledViaApi,
      detail = Map("invitationId" -> invitationId.value, "agentReferenceNumber" -> arn)
        .filter(_._2.nonEmpty) ++ failure.map(e => Seq("failureDescription" -> e)).getOrElse(Seq.empty),
      tags = Map("transactionName" -> "Agent cancelled invitation through third party software"))

  def verifyAgentCheckRelationshipStatusEvent(
    arn: String,
    clientId: String,
    clientIdType: String,
    result: String,
    service: String,
    failure: Option[String] = None): Unit =
    verifyAuditRequestSent(
      1,
      AgentAuthorisationEvent.AgentCheckRelationshipStatusApi,
      detail = Map(
        "result" -> result,
        "agentReferenceNumber" -> arn,
        "clientIdType" -> clientIdType,
        "clientId" -> clientId,
        "service" -> service)
        .filter(_._2.nonEmpty) ++ failure.map(e => Seq("failureDescription" -> e)).getOrElse(Seq.empty),
      tags = Map("transactionName" -> "Agent checked status of relationship through third party software"))
}
