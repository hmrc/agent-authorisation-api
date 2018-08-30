package uk.gov.hmrc.agentauthorisation.connectorsapi

import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.connectors.InvitationsConnector
import uk.gov.hmrc.agentauthorisation.controllers.api.AgentController
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.agentauthorisation.models.AgentInvitation
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{ Arn, InvitationId, Vrn }
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class InvitationsConnectorISpec extends BaseISpec {

  val connector: InvitationsConnector = app.injector.instanceOf[InvitationsConnector]

  private val arn = Arn("TARN0000001")
  private val arn2 = Arn("DARN0002185")
  private val validNino = Nino("AB123456A")
  private val validVrn = Vrn("101747696")
  private val validVatRegDate = "2007-07-07"
  private val validPostcode = "DH14EJ"
  private val invitationIdITSA = InvitationId("ABERULMHCKKW3")
  private val invitationIdVAT = InvitationId("CZTW1KY6RTAAT")
  implicit val hc: HeaderCarrier = HeaderCarrier()

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
}
