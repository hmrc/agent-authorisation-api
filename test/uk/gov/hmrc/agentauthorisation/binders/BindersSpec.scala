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

package uk.gov.hmrc.agentauthorisation.binders

import uk.gov.hmrc.agentauthorisation.support.UnitSpec
import uk.gov.hmrc.agentauthorisation.models.{ApiClientId, ApiService, Arn, InvitationId}

class BindersSpec extends UnitSpec:

  "getInvitationIdBinder.bind" should {
    "return a successful invitationId when the invitationId is valid" in {
      UrlBinders.getInvitationIdBinder
        .bind("invitationId", "ABERULMHCKKW3") shouldBe Right(InvitationId("ABERULMHCKKW3"))
    }

    "return an error when the invitationId is invalid" in {
      UrlBinders.getInvitationIdBinder
        .bind("invitationId", "foo") shouldBe Left(ErrorConstants.InvitationIdInvalid)
    }
  }

  "getInvitationIdBinder.unbind" should {
    "return the invitationId string" in {
      UrlBinders.getInvitationIdBinder.unbind("invitationId", InvitationId("ABERULMHCKKW3")) shouldBe "ABERULMHCKKW3"
    }
  }

  "arnBinder.bind" should {
    "return an ARN when it is valid" in {
      UrlBinders.arnBinder.bind("arn", "TARN0000001") shouldBe Right(Arn("TARN0000001"))
    }

    "return an error when the ARN is invalid" in {
      UrlBinders.arnBinder.bind("arn", "not-an-arn") shouldBe Left(ErrorConstants.ArnInvalid)
    }
  }

  "arnBinder.unbind" should {
    "return the ARN string" in {
      UrlBinders.arnBinder.unbind("arn", Arn("TARN0000001")) shouldBe "TARN0000001"
    }
  }

  "apiServiceBinder" should {
    "bind and unbind a supported service" in {
      UrlBinders.apiServiceBinder.bind("service", "PILLAR2") shouldBe Right(ApiService.Pillar2)
      UrlBinders.apiServiceBinder.unbind("service", ApiService.Pillar2) shouldBe "PILLAR2"
    }

    "reject an unsupported service" in {
      UrlBinders.apiServiceBinder.bind("service", "SA") shouldBe Left(ErrorConstants.ServiceUnsupported)
    }
  }

  "apiClientIdBinder" should {
    "bind and unbind an identifier with a supported format" in {
      val clientId = ApiClientId.from("101747696").value

      UrlBinders.apiClientIdBinder.bind("clientId", clientId.value) shouldBe Right(clientId)
      UrlBinders.apiClientIdBinder.unbind("clientId", clientId) shouldBe "101747696"
    }

    "reject an identifier with no supported format" in {
      UrlBinders.apiClientIdBinder.bind("clientId", "invalid") shouldBe Left(ErrorConstants.ClientIdInvalid)
    }
  }
