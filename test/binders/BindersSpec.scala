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

package binders

import uk.gov.hmrc.agentauthorisation.binders.{ ErrorConstants, UrlBinders }
import uk.gov.hmrc.agentmtdidentifiers.model.InvitationId
import uk.gov.hmrc.play.test.UnitSpec

class BindersSpec extends UnitSpec {

  "getInvitationIdBinder.bind" should {
    "return a successful invitationId when the invitationId is valid" in {
      UrlBinders.getInvitationIdBinder.bind("invitationId", "ABERULMHCKKW3") shouldBe Right(InvitationId("ABERULMHCKKW3"))
    }

    "return an error when the invitationId is invalid" in {
      UrlBinders.getInvitationIdBinder.bind("invitationId", "foo") shouldBe Left(ErrorConstants.InvitationIdNotFound)
    }
  }

  "getInvitationIdBinder.unbind" should {
    "return the invitationId string" in {
      UrlBinders.getInvitationIdBinder.unbind("invitationId", InvitationId("ABERULMHCKKW3")) shouldBe "ABERULMHCKKW3"
    }
  }

}
