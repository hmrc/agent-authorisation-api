/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.connectors

import play.api.test.Helpers._
import uk.gov.hmrc.agentauthorisation.models.{KnownFactCheckFailed, KnownFactCheckPassed}
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class InvitationsConnectorISpec extends BaseISpec {

  val connector: InvitationsConnector = app.injector.instanceOf[InvitationsConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

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

}
