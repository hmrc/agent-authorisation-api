/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.models.v3

import play.api.libs.json.{Format, JsError, JsString, Json}
import uk.gov.hmrc.agentauthorisation.models.{AgentType, ApiService}
import uk.gov.hmrc.agentauthorisation.support.UnitSpec

class ApiInvitationServiceConfigurationSpec extends UnitSpec:

  private val configurations = Seq(
    ("HMRC-MTD-IT", ApiService.MtdIt, "income-tax", Some(AgentType.Main)),
    ("HMRC-MTD-IT-SUPP", ApiService.MtdIt, "income-tax", Some(AgentType.Supporting)),
    ("HMRC-MTD-VAT", ApiService.MtdVat, "vat", None),
    ("HMRC-TERS-ORG", ApiService.Trusts, "trusts-and-estates", None),
    ("HMRC-TERSNT-ORG", ApiService.Trusts, "trusts-and-estates", None),
    ("PERSONAL-INCOME-RECORD", ApiService.Irv, "income-record-viewer", None),
    ("HMRC-CGT-PD", ApiService.CgtPd, "capital-gains-tax-uk-property", None),
    ("HMRC-PPT-ORG", ApiService.Ppt, "plastic-packaging-tax", None),
    ("HMRC-CBC-ORG", ApiService.Cbc, "country-by-country-reporting", None),
    ("HMRC-CBC-NONUK-ORG", ApiService.Cbc, "country-by-country-reporting", None),
    ("HMRC-PILLAR2-ORG", ApiService.Pillar2, "pillar-2", None)
  )

  "forAcrInvitationService" should:
    "map every ACR invitation service to its V3 representation" in:
      configurations.foreach: (acrService, apiService, urlPart, agentType) =>
        ApiServiceConfiguration.forAcrInvitationService(acrService) shouldBe Some(
          ApiInvitationServiceConfiguration(apiService, urlPart, agentType)
        )

    "reject an unsupported ACR invitation service" in:
      ApiServiceConfiguration.forAcrInvitationService("UNKNOWN") shouldBe None

  "ApiInvitationStatus JSON" should:
    "round-trip every public status with its specified spelling" in:
      ApiInvitationStatus.values.foreach: status =>
        Json.toJson(status).as[ApiInvitationStatus] shouldBe status

    "reject an unsupported status" in:
      summon[Format[ApiInvitationStatus]].reads(JsString("Declined")) shouldBe a[JsError]
