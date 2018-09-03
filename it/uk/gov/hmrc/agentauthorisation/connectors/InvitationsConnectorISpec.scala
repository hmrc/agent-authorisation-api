package uk.gov.hmrc.agentauthorisation.connectors

import org.joda.time.LocalDate
import uk.gov.hmrc.agentauthorisation._
import uk.gov.hmrc.agentauthorisation.models.{ AgentInvitation, StoredInvitation }
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class InvitationsConnectorISpec extends BaseISpec {

  val connector: InvitationsConnector = app.injector.instanceOf[InvitationsConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val storedItsaInvitation = StoredInvitation(
    s"$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent/ABERULMHCKKW3",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    "2017-10-31T23:22:50.971Z",
    Arn("TARN0000001"), "MTD-IT", "Pending")

  val storedVatInvitation = StoredInvitation(
    s"$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent/CZTW1KY6RTAAT",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    "2017-10-31T23:22:50.971Z",
    Arn("TARN0000001"), "MTD-VAT", "Pending")

  "createInvitation" should {

    "return a url upon success for ITSA" in {
      createInvitationStub(arn, validNino.value, invitationIdITSA, validNino.value, "ni", "HMRC-MTD-IT", "NI", validPostcode)
      val agentInvitation = AgentInvitation("HMRC-MTD-IT", "ni", validNino.value, validPostcode)
      val result = await(connector.createInvitation(arn, agentInvitation))
      result.get should include(s"/agent-client-authorisation/clients/NI/AB123456A/invitations/received/${invitationIdITSA.value}")
    }

    "return a url upon success for VAT" in {
      createInvitationStub(arn, validVrn.value, invitationIdVAT, validVrn.value, "vrn", "HMRC-MTD-VAT", "VRN", validVatRegDate)
      val agentInvitation = AgentInvitation("HMRC-MTD-VAT", "vrn", validVrn.value, validVatRegDate)
      val result = await(connector.createInvitation(arn, agentInvitation))
      result.get should include(s"/agent-client-authorisation/clients/VRN/101747696/invitations/received/${invitationIdVAT.value}")
    }
  }

  "checkPostcodeForClient" should {
    "return true when the nino and postcode do match" in {
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      val result = await(connector.checkPostcodeForClient(validNino, validPostcode))

      result shouldBe Some(true)
    }

    "return false when the nino and postcode do not match" in {
      givenNonMatchingClientIdAndPostcode(validNino, validPostcode)
      val result = await(connector.checkPostcodeForClient(validNino, validPostcode))

      result shouldBe Some(false)
    }

    "return None when the client registration is not found" in {
      givenNotEnrolledClientITSA(validNino, validPostcode)
      val result = await(connector.checkPostcodeForClient(validNino, validPostcode))

      result shouldBe None
    }
  }

  "checkVatRegDateForClient" should {
    "return true when the Vrn and VAT registration date do match" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
      val result = await(connector.checkVatRegDateForClient(validVrn, LocalDate.parse(validVatRegDate)))

      result shouldBe Some(true)
    }

    "return false when the Vrn and VAT registration date do not match" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 403)
      val result = await(connector.checkVatRegDateForClient(validVrn, LocalDate.parse(validVatRegDate)))

      result shouldBe Some(false)
    }

    "return None when the client registration is not found" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 404)
      val result = await(connector.checkVatRegDateForClient(validVrn, LocalDate.parse(validVatRegDate)))

      result shouldBe None
    }
  }

  "getInvitation" should {
    "return an ITSA invitation" in {
      givenGetITSAInvitationStub(arn, "Pending")
      val result = await(connector.getInvitation(arn, invitationIdITSA))

      result.get shouldBe storedItsaInvitation
    }

    "return an VAT invitation" in {
      givenGetVATInvitationStub(arn, "Pending")
      val result = await(connector.getInvitation(arn, invitationIdVAT))

      result.get shouldBe storedVatInvitation
    }

    "return no invitation" in {
      givenInvitationNotFound(arn, invitationIdITSA)
      val result = await(connector.getInvitation(arn, invitationIdITSA))

      result shouldBe None
    }
  }

  "cancelInvitation" should {
    "return 204 when cancellation is successful" in {
      givenCancelAgentInvitationStub(arn, invitationIdITSA, 204)
      val result = await(connector.cancelInvitation(arn, invitationIdITSA))

      result shouldBe Some(204)
    }

    "return 404 when invitation is not found" in {
      givenCancelAgentInvitationStub(arn, invitationIdITSA, 404)
      val result = await(connector.cancelInvitation(arn, invitationIdITSA))

      result shouldBe Some(404)
    }

    "return 500 when an invitation cannot be cancelled" in {
      givenCancelAgentInvitationStub(arn, invitationIdITSA, 500)
      val result = await(connector.cancelInvitation(arn, invitationIdITSA))

      result shouldBe Some(500)
    }

    "return None when some other response is returned" in {
      givenCancelAgentInvitationStub(arn, invitationIdITSA, 403)
      val result = await(connector.cancelInvitation(arn, invitationIdITSA))

      result shouldBe None
    }
  }
}
