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

import uk.gov.hmrc.agentauthorisation.models.{AgentType, ApiClientId, ApiService}
import uk.gov.hmrc.agentauthorisation.models.ApiClientId.value

final case class ApiServiceConfiguration(
  detailsService: String,
  suppliedClientIdType: String,
  acceptedKnownFactTypes: Set[ApiKnownFactType]
):
  def createService(command: ValidatedCreateInvitationV3, details: AcrClientDetails): String =
    command.service match
      case ApiService.MtdIt if command.agentType.contains(AgentType.Supporting) => "HMRC-MTD-IT-SUPP"
      case ApiService.Cbc if details.isOverseas.contains(true)                 => "HMRC-CBC-NONUK-ORG"
      case _                                                                  => detailsService

final case class ApiInvitationServiceConfiguration(
  service: ApiService,
  clientActionUrlPart: String,
  agentType: Option[AgentType] = None
)

object ApiServiceConfiguration:
  private val postalOrCountry = Set(ApiKnownFactType.PostalCode, ApiKnownFactType.CountryCode)

  def forCommand(service: ApiService, clientId: ApiClientId): ApiServiceConfiguration =
    service match
      case ApiService.MtdIt  => ApiServiceConfiguration("HMRC-MTD-IT", "ni", postalOrCountry)
      case ApiService.MtdVat => ApiServiceConfiguration("HMRC-MTD-VAT", "vrn", Set(ApiKnownFactType.Date))
      case ApiService.Trusts if clientId.value.forall(_.isDigit) =>
        ApiServiceConfiguration("HMRC-TERS-ORG", "utr", Set.empty)
      case ApiService.Trusts => ApiServiceConfiguration("HMRC-TERSNT-ORG", "urn", Set.empty)
      case ApiService.Irv    => ApiServiceConfiguration("PERSONAL-INCOME-RECORD", "ni", Set(ApiKnownFactType.Date))
      case ApiService.CgtPd  => ApiServiceConfiguration("HMRC-CGT-PD", "CGTPDRef", postalOrCountry)
      case ApiService.Ppt    => ApiServiceConfiguration("HMRC-PPT-ORG", "EtmpRegistrationNumber", Set(ApiKnownFactType.Date))
      case ApiService.Cbc    => ApiServiceConfiguration("HMRC-CBC-ORG", "cbcId", Set(ApiKnownFactType.Email))
      case ApiService.Pillar2 => ApiServiceConfiguration("HMRC-PILLAR2-ORG", "PLRID", Set(ApiKnownFactType.Date))

  def forAcrInvitationService(service: String): Option[ApiInvitationServiceConfiguration] =
    service match
      case "HMRC-MTD-IT" =>
        Some(ApiInvitationServiceConfiguration(ApiService.MtdIt, "income-tax", Some(AgentType.Main)))
      case "HMRC-MTD-IT-SUPP" =>
        Some(ApiInvitationServiceConfiguration(ApiService.MtdIt, "income-tax", Some(AgentType.Supporting)))
      case "HMRC-MTD-VAT" =>
        Some(ApiInvitationServiceConfiguration(ApiService.MtdVat, "vat"))
      case "HMRC-TERS-ORG" | "HMRC-TERSNT-ORG" =>
        Some(ApiInvitationServiceConfiguration(ApiService.Trusts, "trusts-and-estates"))
      case "PERSONAL-INCOME-RECORD" =>
        Some(ApiInvitationServiceConfiguration(ApiService.Irv, "income-record-viewer"))
      case "HMRC-CGT-PD" =>
        Some(ApiInvitationServiceConfiguration(ApiService.CgtPd, "capital-gains-tax-uk-property"))
      case "HMRC-PPT-ORG" =>
        Some(ApiInvitationServiceConfiguration(ApiService.Ppt, "plastic-packaging-tax"))
      case "HMRC-CBC-ORG" | "HMRC-CBC-NONUK-ORG" =>
        Some(ApiInvitationServiceConfiguration(ApiService.Cbc, "country-by-country-reporting"))
      case "HMRC-PILLAR2-ORG" =>
        Some(ApiInvitationServiceConfiguration(ApiService.Pillar2, "pillar-2"))
      case _ => None
