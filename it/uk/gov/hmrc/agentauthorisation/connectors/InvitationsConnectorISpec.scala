package uk.gov.hmrc.agentauthorisation.connectors

import play.api.test.Helpers._

import java.time.{LocalDate, ZoneOffset}
import uk.gov.hmrc.agentauthorisation.models.ClientType.{business, personal}
import uk.gov.hmrc.agentauthorisation.models.Service.{Itsa, Vat}
import uk.gov.hmrc.agentauthorisation.models.{AgentInvitation, KnownFactCheckFailed, KnownFactCheckPassed, StoredInvitation}
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class InvitationsConnectorISpec extends BaseISpec {

  val connector: InvitationsConnector = app.injector.instanceOf[InvitationsConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val storedItsaInvitation = StoredInvitation(
    s"$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent/ABERULMHCKKW3",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    "2018-09-11T21:02:00.000Z",
    Arn("TARN0000001"),
    Some("personal"),
    "MTD-IT",
    "Pending",
    Some("someInvitationUrl/invitations/personal/12345678/agent-1")
  )

  val storedVatInvitation = StoredInvitation(
    s"$wireMockBaseUrl/agent-client-authorisation/agencies/TARN0000001/invitations/sent/CZTW1KY6RTAAT",
    "2017-10-31T23:22:50.971Z",
    "2017-12-18T00:00:00.000",
    "2018-09-11T21:02:00.000Z",
    Arn("TARN0000001"),
    Some("business"),
    "MTD-VAT",
    "Pending",
    Some("someInvitationUrl/invitations/business/12345678/agent-1")
  )

  val storedInvitations = Seq(storedItsaInvitation, storedVatInvitation)

  "createInvitation" should {

    "return a Invitation Id upon success for ITSA" in {
      createInvitationStub(
        arn,
        validNino.value,
        invitationIdITSA,
        validNino.value,
        "ni",
        "personal",
        "HMRC-MTD-IT",
        "NI",
        validPostcode
      )
      val agentInvitation = AgentInvitation(Itsa, personal, "ni", "AB123456A", "DH14EJ", Some("main"))
      val result = await(connector.createInvitation(arn, agentInvitation))
      result.get should include(invitationIdITSA.value)
    }

    "return a Invitation Id upon success for VAT" in {
      createInvitationStub(
        arn,
        validVrn.value,
        invitationIdVAT,
        validVrn.value,
        "vrn",
        "business",
        "HMRC-MTD-VAT",
        "VRN",
        validVatRegDate
      )
      val agentInvitation = AgentInvitation(Vat, business, "vrn", validVrn.value, validVatRegDate, None)
      val result = await(connector.createInvitation(arn, agentInvitation))
      result.get should include(invitationIdVAT.value)
    }
  }

  "checkPostcodeForClient" should {
    "return KnownFactCheckPassed when the nino and postcode do match" in {
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      val result = await(connector.checkPostcodeForClient(validNino, validPostcode))

      result shouldBe KnownFactCheckPassed
    }

    "return KnownFactCheckFailed when the nino and postcode do not match" in {
      givenNonMatchingClientIdAndPostcode(validNino, validPostcode)
      val result = await(connector.checkPostcodeForClient(validNino, validPostcode))

      result shouldBe KnownFactCheckFailed("POSTCODE_DOES_NOT_MATCH")
    }

    "return KnownFactFailed when the client registration is not found" in {
      givenNotEnrolledClientITSA(validNino, validPostcode)
      val result = await(connector.checkPostcodeForClient(validNino, validPostcode))

      result shouldBe KnownFactCheckFailed("CLIENT_REGISTRATION_NOT_FOUND")
    }
  }

  "checkVatRegDateForClient" should {
    "return KnownFactCheckPassed when the Vrn and VAT registration date do match" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 204)
      val result = await(connector.checkVatRegDateForClient(validVrn, LocalDate.parse(validVatRegDate)))

      result shouldBe KnownFactCheckPassed
    }

    "return KnownFactCheckFailed when the Vrn and VAT registration date do not match" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 403)
      val result = await(connector.checkVatRegDateForClient(validVrn, LocalDate.parse(validVatRegDate)))

      result shouldBe KnownFactCheckFailed("VAT_REGISTRATION_DATE_DOES_NOT_MATCH")
    }

    "return KnownFactCheckFailed when the check returns a Locked response" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 423)
      val result = await(connector.checkVatRegDateForClient(validVrn, LocalDate.parse(validVatRegDate)))

      result shouldBe KnownFactCheckFailed("MIGRATION_IN_PROGRESS")
    }

    "return KnownFactCheckFailed when the client registration is not found" in {
      checkClientIdAndVatRegDate(validVrn, LocalDate.parse(validVatRegDate), 404)
      val result = await(connector.checkVatRegDateForClient(validVrn, LocalDate.parse(validVatRegDate)))

      result shouldBe KnownFactCheckFailed("VAT_RECORD_NOT_FOUND")
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
      givenCancelAgentInvitationStubInvalid(arn, invitationIdITSA)
      val result = await(connector.cancelInvitation(arn, invitationIdITSA))

      result shouldBe Some(500)
    }

    "return None when some other response is returned" in {
      givenCancelAgentInvitationStub(arn, invitationIdITSA, 403)
      val result = await(connector.cancelInvitation(arn, invitationIdITSA))

      result shouldBe Some(403)
    }
  }

  "getAllInvitations" should {
    "return a sequence of stored invitations" in {
      givenInvitationsServiceReturns(arn, Seq(itsa(arn), vat(arn)))
      val result = await(connector.getAllInvitations(arn, LocalDate.now(ZoneOffset.UTC).minusDays(30)))

      result shouldBe storedInvitations
    }

    "return a empty sequence of stored invitations" in {
      givenAllInvitationsEmptyStub(arn)
      val result: Seq[StoredInvitation] =
        await(connector.getAllInvitations(arn, LocalDate.now(ZoneOffset.UTC).minusDays(30)))

      result shouldBe Seq.empty[StoredInvitation]
    }
  }

  "getAllInvitationsForClient" should {
    "return non empty when invitations exist" in {
      givenOnlyPendingInvitationsExistForClient(arn, Nino(nino), "HMRC-MTD-IT")
      val result = await(connector.getAllInvitationsForClient(arn, nino, Itsa))

      assert(result.nonEmpty)
    }

    "return empty when invitations do not exist" in {
      givenNoInvitationsExistForClient(arn, Nino(nino), "HMRC-MTD-IT")
      val result = await(connector.getAllInvitationsForClient(arn, nino, Itsa))

      assert(result.isEmpty)
    }
  }
}
