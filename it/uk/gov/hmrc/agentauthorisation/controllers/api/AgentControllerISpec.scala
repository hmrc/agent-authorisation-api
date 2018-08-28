package uk.gov.hmrc.agentauthorisation.controllers.api

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{ Arn, InvitationId }
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._

class AgentControllerISpec extends BaseISpec {

  lazy val controller: AgentController = app.injector.instanceOf[AgentController]
  private val arn = Arn("TARN0000001")
  private val arn2 = Arn("DARN0002185")
  private val validNino = Nino("AB123456A")
  private val validPostcode = "DH14EJ"
  private val invitationIdITSA = InvitationId("ABERULMHCKKW3")
  val jsonBody = Json.parse(
    s"""{"service": "MTD-IT", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}""")

  "/agents/:arn/invitations" should {

    val request = FakeRequest("POST", s"/agents/${arn.value}/invitations")
    val createInvitation = controller.createInvitationApi(arn)

    "return 204 when invitation is successfully created for ITSA" in {
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      createInvitationStub(arn, validNino.value, invitationIdITSA, validNino.value, "ni", "HMRC-MTD-IT", "MTDITID", validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBody), arn.value))

      status(result) shouldBe 204
      result.header.headers("Location") should include(routes.AgentController.getInvitationApi(arn, invitationIdITSA).url)
    }

    "return 400 SERVICE_NOT_SUPPORTED when the service is not supported" in {
      val jsonBodyInvalidService = Json.parse(
        s"""{"service": "foo", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe UnsupportedService
    }

    "return 400 CLIENT_ID_FORMAT_INVALID when the clientId has an invalid format" in {
      val jsonBodyInvalidService = Json.parse(
        s"""{"service": "MTD-IT", "clientIdType": "ni", "clientId": "foo", "knownFact": "$validPostcode"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe InvalidItsaNino
    }

    "return 400 POSTCODE_FORMAT_INVALID when the postcode has an invalid format" in {
      val jsonBodyInvalidService = Json.parse(
        s"""{"service": "MTD-IT", "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "foo"}""")

      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyInvalidService), arn.value))

      status(result) shouldBe 400
      await(result) shouldBe PostcodeFormatInvalid

    }

    "return 403 CLIENT_REGISTRATION_NOT_FOUND when the postcode returns nothing" in {
      givenNotEnrolledClientITSA(validNino, validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBody), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe ClientRegistrationNotFound
    }

    "return 403 POSTCODE_DOES_NOT_MATCH when the postcode and clientId do not match" in {
      givenNonMatchingClientIdAndPostcode(validNino, validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBody), arn.value))

      status(result) shouldBe 403
      await(result) shouldBe PostcodeDoesNotMatch
    }

    "return 403 NOT_AN_AGENT when the logged in user is not have an HMRC-AS-AGENT enrolment" in {
      givenUnauthorisedForInsufficientEnrolments()
      val result = createInvitation(request.withJsonBody(jsonBody))

      status(result) shouldBe 403
      await(result) shouldBe NotAnAgent
    }

    "return 403 NOT_PERMISSION_ON_AGENCY when the logged in user does not have an HMRC-AS-AGENT enrolment" in {
      createInvitationStub(arn, validNino.value, invitationIdITSA, validNino.value, "ni", "HMRC-MTD-IT", "MTDITID", validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBody), arn2.value))

      status(result) shouldBe 403
      await(result) shouldBe NoPermissionOnAgency
    }

    "return a future failed when the invitation creation failed" in {
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      failedCreateInvitation(arn)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBody), arn.value))

      an[Exception] shouldBe thrownBy {
        await(result)
      }
    }
  }
}
