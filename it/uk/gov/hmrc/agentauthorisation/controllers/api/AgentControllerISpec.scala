package uk.gov.hmrc.agentauthorisation.controllers.api

import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{ Arn, InvitationId, Vrn }
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._

class AgentControllerISpec extends BaseISpec {

  lazy val controller: AgentController = app.injector.instanceOf[AgentController]
  private val arn = Arn("TARN0000001")
  private val arn2 = Arn("DARN0002185")
  private val validNino = Nino("AB123456A")
  private val validPostcode = "DH14EJ"
  private val validVrn = Vrn("101747696")
  private val validVatRegDate = "2007-07-07"
  private val invitationIdITSA = InvitationId("ABERULMHCKKW3")
  private val invitationIdVAT = InvitationId("CZTW1KY6RTAAT")
  val jsonBodyITSA = Json.parse(
    s"""{"service": ["MTD-IT"], "clientIdType": "ni", "clientId": "${validNino.value}", "knownFact": "$validPostcode"}""")
  val jsonBodyVAT = Json.parse(
    s"""{"service": ["MTD-VAT"], "clientIdType": "vrn", "clientId": "${validVrn.value}", "knownFact": "$validVatRegDate"}""")

  "/agents/:arn/invitations" should {

    val request = FakeRequest("POST", s"/agents/${arn.value}/invitations")
    val createInvitation = controller.createInvitationApi(arn)

    "return 204 when invitation is successfully created for ITSA" in {
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      createInvitationStub(arn, validNino.value, invitationIdITSA, validNino.value, "ni", "HMRC-MTD-IT", "MTDITID", validPostcode)
      val result = createInvitation(authorisedAsValidAgent(request.withJsonBody(jsonBodyITSA), arn.value))

      status(result) shouldBe 204
      result.header.headers("Location") shouldBe (routes.AgentController.getInvitationApi(arn, invitationIdITSA).url)
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

      status(result) shouldBe 403
      await(result) shouldBe NotAnAgent
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
}
