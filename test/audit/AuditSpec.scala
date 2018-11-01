/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package audit

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.audit.AuditService
import uk.gov.hmrc.agentauthorisation.models.AgentInvitation
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.{Authorization, RequestId, SessionId}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext

class AuditSpec extends UnitSpec with MockitoSugar with Eventually {

  "auditEvent" should {

    "send an AgentAuthorisationCreatedViaApi Event for ITSA" in {
      val mockConnector = mock[AuditConnector]
      val service = new AuditService(mockConnector)

      val hc = HeaderCarrier(
        authorization = Some(Authorization("dummy bearer token")),
        sessionId = Some(SessionId("dummy session id")),
        requestId = Some(RequestId("dummy request id")))

      val arn: Arn = Arn("HX2345")
      val agentInvitation: AgentInvitation =
        AgentInvitation("HMRC-MTD-IT", "ni", "AB123456A", "DH14EJ")
      val invitationId: String = "1"
      val result: String = "Success"

      await(
        service
          .sendAgentInvitationSubmitted(arn, invitationId, agentInvitation, result)(hc, FakeRequest("GET", "/path")))

      eventually {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(mockConnector).sendEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext])
        val sentEvent = captor.getValue.asInstanceOf[DataEvent]

        sentEvent.auditType shouldBe "AgentAuthorisationCreatedViaApi"
        sentEvent.auditSource shouldBe "agent-authorisation-api"
        sentEvent.detail("factCheck") shouldBe "Success"
        sentEvent.detail("invitationId") shouldBe "1"
        sentEvent.detail("agentReferenceNumber") shouldBe "HX2345"
        sentEvent.detail("clientIdType") shouldBe "ni"
        sentEvent.detail("clientId") shouldBe "AB123456A"
        sentEvent.detail("service") shouldBe "HMRC-MTD-IT"

        sentEvent.tags.contains("Authorization") shouldBe false
        sentEvent.detail("Authorization") shouldBe "dummy bearer token"

        sentEvent.tags("transactionName") shouldBe "Agent created invitation through third party software"
        sentEvent.tags("path") shouldBe "/path"
        sentEvent.tags("X-Session-ID") shouldBe "dummy session id"
        sentEvent.tags("X-Request-ID") shouldBe "dummy request id"
      }
    }

    "send an AgentAuthorisationCreatedViaApi Event for VAT" in {
      val mockConnector = mock[AuditConnector]
      val service = new AuditService(mockConnector)

      val hc = HeaderCarrier(
        authorization = Some(Authorization("dummy bearer token")),
        sessionId = Some(SessionId("dummy session id")),
        requestId = Some(RequestId("dummy request id")))

      val arn: Arn = Arn("HX2345")
      val agentInvitation: AgentInvitation =
        AgentInvitation("HMRC-MTD-VAT", "vrn", "101747641", "2008-08-08")
      val invitationId: String = "1"
      val result: String = "Success"

      await(
        service
          .sendAgentInvitationSubmitted(arn, invitationId, agentInvitation, result)(hc, FakeRequest("GET", "/path")))

      eventually {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(mockConnector).sendEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext])
        val sentEvent = captor.getValue.asInstanceOf[DataEvent]

        sentEvent.auditType shouldBe "AgentAuthorisationCreatedViaApi"
        sentEvent.auditSource shouldBe "agent-authorisation-api"
        sentEvent.detail("factCheck") shouldBe "Success"
        sentEvent.detail("invitationId") shouldBe "1"
        sentEvent.detail("agentReferenceNumber") shouldBe "HX2345"
        sentEvent.detail("clientIdType") shouldBe "vrn"
        sentEvent.detail("clientId") shouldBe "101747641"
        sentEvent.detail("service") shouldBe "HMRC-MTD-VAT"

        sentEvent.tags.contains("Authorization") shouldBe false
        sentEvent.detail("Authorization") shouldBe "dummy bearer token"

        sentEvent.tags("transactionName") shouldBe "Agent created invitation through third party software"
        sentEvent.tags("path") shouldBe "/path"
        sentEvent.tags("X-Session-ID") shouldBe "dummy session id"
        sentEvent.tags("X-Request-ID") shouldBe "dummy request id"
      }
    }

    "send an AgentAuthorisedCancelledViaApi Event for ITSA" in {
      val mockConnector = mock[AuditConnector]
      val service = new AuditService(mockConnector)

      val hc = HeaderCarrier(
        authorization = Some(Authorization("dummy bearer token")),
        sessionId = Some(SessionId("dummy session id")),
        requestId = Some(RequestId("dummy request id")))

      val arn: Arn = Arn("HX2345")
      val invitationId: String = "1"
      val result: String = "Success"

      await(service.sendAgentInvitationCancelled(arn, invitationId, result)(hc, FakeRequest("GET", "/path")))

      eventually {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(mockConnector).sendEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext])
        val sentEvent = captor.getValue.asInstanceOf[DataEvent]

        sentEvent.auditType shouldBe "AgentAuthorisedCancelledViaApi"
        sentEvent.auditSource shouldBe "agent-authorisation-api"
        sentEvent.detail("invitationId") shouldBe "1"
        sentEvent.detail("agentReferenceNumber") shouldBe "HX2345"

        sentEvent.tags.contains("Authorization") shouldBe false
        sentEvent.detail("Authorization") shouldBe "dummy bearer token"

        sentEvent.tags("transactionName") shouldBe "Agent cancelled invitation through third party software"
        sentEvent.tags("path") shouldBe "/path"
        sentEvent.tags("X-Session-ID") shouldBe "dummy session id"
        sentEvent.tags("X-Request-ID") shouldBe "dummy request id"
      }
    }

    "send an AgentCheckRelationshipStatusApi Event for ITSA" in {
      val mockConnector = mock[AuditConnector]
      val service = new AuditService(mockConnector)

      val hc = HeaderCarrier(
        authorization = Some(Authorization("dummy bearer token")),
        sessionId = Some(SessionId("dummy session id")),
        requestId = Some(RequestId("dummy request id")))

      val arn: Arn = Arn("HX2345")
      val agentInvitation: AgentInvitation =
        AgentInvitation("HMRC-MTD-IT", "ni", "AB123456A", "DH14EJ")
      val result: String = "Success"

      await(service.sendAgentCheckRelationshipStatus(arn, agentInvitation, result)(hc, FakeRequest("POST", "/path")))

      eventually {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(mockConnector).sendEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext])
        val sentEvent = captor.getValue.asInstanceOf[DataEvent]

        sentEvent.auditType shouldBe "AgentCheckRelationshipStatusApi"
        sentEvent.auditSource shouldBe "agent-authorisation-api"
        sentEvent.detail("agentReferenceNumber") shouldBe "HX2345"
        sentEvent.detail("clientIdType") shouldBe "ni"
        sentEvent.detail("clientId") shouldBe "AB123456A"
        sentEvent.detail("service") shouldBe "HMRC-MTD-IT"

        sentEvent.tags.contains("Authorization") shouldBe false
        sentEvent.detail("Authorization") shouldBe "dummy bearer token"

        sentEvent.tags("transactionName") shouldBe "Agent checked status of relationship through third party software"
        sentEvent.tags("path") shouldBe "/path"
        sentEvent.tags("X-Session-ID") shouldBe "dummy session id"
        sentEvent.tags("X-Request-ID") shouldBe "dummy request id"
      }
    }
  }
}
