/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.audit

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentauthorisation.models.Arn
import uk.gov.hmrc.agentauthorisation.support.UnitSpec
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, RequestId, SessionId}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext

class AuditSpec extends UnitSpec with MockitoSugar with Eventually {

  private def sentEventFor(failure: Option[String] = None): DataEvent = {
    val mockConnector = mock[AuditConnector]
    val service = new AuditService(mockConnector)

    given HeaderCarrier = HeaderCarrier(
      authorization = Some(Authorization("dummy bearer token")),
      sessionId = Some(SessionId("dummy session id")),
      requestId = Some(RequestId("dummy request id"))
    )
    given ExecutionContext = ExecutionContext.Implicits.global
    given FakeRequest[play.api.mvc.AnyContentAsEmpty.type] = FakeRequest("GET", "/path")

    await(service.sendAgentInvitationCancelled(Arn("HX2345"), "1", "Success", failure))

    eventually {
      val captor = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(mockConnector).sendEvent(captor.capture())(using any[HeaderCarrier], any[ExecutionContext])
      captor.getValue
    }
  }

  "auditEvent" should {

    "send an agentAuthorisedCancelledViaApi Event for ITSA" in {
      val sentEvent = sentEventFor()

      sentEvent.auditType shouldBe "agentAuthorisedCancelledViaApi"
      sentEvent.auditSource shouldBe "agent-authorisation-api"
      sentEvent.detail("result") shouldBe "Success"
      sentEvent.detail("invitationId") shouldBe "1"
      sentEvent.detail("agentReferenceNumber") shouldBe "HX2345"
      sentEvent.detail.contains("failureDescription") shouldBe false

      sentEvent.tags("transactionName") shouldBe "agent-cancelled-invitation-via-api"
      sentEvent.tags("path") shouldBe "/path"
      sentEvent.tags("X-Session-ID") shouldBe "dummy session id"
      sentEvent.tags("X-Request-ID") shouldBe "dummy request id"
    }

    "include failure description when one is supplied" in {
      val sentEvent = sentEventFor(Some("Known failure"))

      sentEvent.detail("failureDescription") shouldBe "Known failure"
    }
  }
}
