package uk.gov.hmrc.agentauthorisation.connectorsapi

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.connectors.InvitationsConnector
import uk.gov.hmrc.agentauthorisation.controllers.api.AgentController
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.agentauthorisation.models.AgentInvitation
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{ Arn, InvitationId }
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

class InvitationsConnectorISpec extends BaseISpec {

  val connector: InvitationsConnector = app.injector.instanceOf[InvitationsConnector]

  private val arn = Arn("TARN0000001")
  private val arn2 = Arn("DARN0002185")
  private val validNino = Nino("AB123456A")
  private val validPostcode = "DH14EJ"
  private val invitationIdITSA = InvitationId("ABERULMHCKKW3")
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "createInvitation" should {
    "return a url upon success" in {
      createInvitationStub(arn, validNino.value, invitationIdITSA, validNino.value, "ni", "HMRC-MTD-IT", "NI", validPostcode)
      val agentInvitation = AgentInvitation("HMRC-MTD-IT", "ni", validNino.value, validPostcode)
      val result = await(connector.createInvitation(arn, agentInvitation))
      result.get should include(s"/agent-client-authorisation/clients/NI/AB123456A/invitations/received/${invitationIdITSA.value}")
    }
  }

  "checkPostcodeForClient" should {
    "return true when the clientId and postcode do match" in {
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      val result = await(connector.checkPostcodeForClient(validNino, validPostcode))

      result shouldBe Some(true)
    }

    "return false when the clientId and postcode do not match" in {
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
}
