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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.testkit.NoMaterializer
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentauthorisation.ErrorHandler
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent
import org.mockito.ArgumentMatchers._
import play.api.Configuration
import play.api.mvc.AnyContentAsEmpty
import uk.gov.hmrc.agentauthorisation.binders.ErrorConstants
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class ErrorHandlerSpec extends UnitSpec with MockitoSugar:
  class BaseSetup(v3Enabled: Boolean = false) {
    given ActorSystem = ActorSystem("MyTest")
    given NoMaterializer.type = NoMaterializer
    given Configuration = Configuration(
      "bootstrap.errorHandler.warnOnly.statusCodes"     -> List(400, 404),
      "bootstrap.errorHandler.suppress4xxErrorMessages" -> false,
      "bootstrap.errorHandler.suppress5xxErrorMessages" -> false,
      "features.enable-v3"                              -> v3Enabled
    )

    given FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val mockAuditConnector = mock[AuditConnector]
    val mockAuditResult = mock[AuditResult]
    val mockHttpAuditEvent = mock[HttpAuditEvent]

    when(mockAuditConnector.sendEvent(any[DataEvent]())(using any[HeaderCarrier](), any[ExecutionContext]()))
      .thenReturn(Future.successful(mockAuditResult))

    val errorHandler = new ErrorHandler(mockAuditConnector, mockHttpAuditEvent)
  }

  "onClientError" should {
    class Setup(statusCode: Int) extends BaseSetup {
      private val fakeRequest = summon[FakeRequest[AnyContentAsEmpty.type]]
      val response = await(errorHandler.onClientError(fakeRequest, statusCode, "A message"))
    }

    "return ErrorNotFound on 404 Not Found" in new Setup(NOT_FOUND) {
      contentAsJson(response) shouldBe StandardNotFound.toJson
    }

    "return ErrorGenericBadRequest on 400 Bad Request" in new Setup(BAD_REQUEST) {
      contentAsJson(response) shouldBe StandardBadRequest.toJson
    }

    "return ARN_FORMAT_INVALID when ARN binding fails" in new BaseSetup {
      val response = await(
        errorHandler.onClientError(
          summon[FakeRequest[AnyContentAsEmpty.type]],
          BAD_REQUEST,
          ErrorConstants.ArnInvalid
        )
      )

      contentAsJson(response) shouldBe ArnInvalidFormat.toJson
    }

    "return INVITATION_ID_FORMAT_INVALID when invitation ID binding fails" in new BaseSetup {
      val response = await(
        errorHandler.onClientError(
          summon[FakeRequest[AnyContentAsEmpty.type]],
          BAD_REQUEST,
          ErrorConstants.InvitationIdInvalid
        )
      )

      contentAsJson(response) shouldBe InvitationIdInvalidFormat.toJson
    }

    "return SERVICE_NOT_SUPPORTED when service binding fails" in new BaseSetup {
      val response = await(
        errorHandler.onClientError(
          summon[FakeRequest[AnyContentAsEmpty.type]],
          BAD_REQUEST,
          ErrorConstants.ServiceUnsupported
        )
      )

      contentAsJson(response) shouldBe UnsupportedService.toJson
    }

    "return CLIENT_ID_FORMAT_INVALID when client ID binding fails" in new BaseSetup {
      val response = await(
        errorHandler.onClientError(
          summon[FakeRequest[AnyContentAsEmpty.type]],
          BAD_REQUEST,
          ErrorConstants.ClientIdInvalid
        )
      )

      contentAsJson(response) shouldBe ClientIdInvalidFormat.toJson
    }

    "return INVALID_PAYLOAD for malformed JSON selected for V3" in new BaseSetup(v3Enabled = true) {
      val request = summon[FakeRequest[AnyContentAsEmpty.type]]
        .withHeaders(ACCEPT -> ApiVersion.V3AcceptHeader)
      val response = await(errorHandler.onClientError(request, BAD_REQUEST, "Invalid Json: malformed"))

      contentAsJson(response) shouldBe InvalidPayload.toJson
    }

    "retain BAD_REQUEST for malformed JSON when V3 is not selected" in new BaseSetup {
      val request = summon[FakeRequest[AnyContentAsEmpty.type]]
        .withHeaders(ACCEPT -> ApiVersion.V3AcceptHeader)
      val response = await(errorHandler.onClientError(request, BAD_REQUEST, "Invalid Json: malformed"))

      contentAsJson(response) shouldBe StandardBadRequest.toJson
    }

    "return ErrorUnauthorized on 401 Unauthorized" in new Setup(UNAUTHORIZED) {
      contentAsJson(response) shouldBe StandardUnauthorised.toJson
    }

    "return a statusCode of 405 with the provided message on 405 Method Not Allowed" in new Setup(METHOD_NOT_ALLOWED) {
      contentAsJson(response) shouldBe Json.obj("statusCode" -> METHOD_NOT_ALLOWED, "message" -> "A message")
    }
  }

  "onServerError" should {
    "return ErrorInternalServerError" in new BaseSetup {
      val response = await(
        errorHandler.onServerError(summon[FakeRequest[AnyContentAsEmpty.type]], new RuntimeException("Internal Server Error"))
      )

      contentAsJson(response) shouldBe StandardInternalServerError.toJson
    }
  }
