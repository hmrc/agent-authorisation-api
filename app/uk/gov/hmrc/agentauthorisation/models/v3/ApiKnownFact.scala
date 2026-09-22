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

import java.time.LocalDate
import scala.util.Try
import scala.util.matching.Regex

enum ApiKnownFactType:
  case PostalCode, CountryCode, Email, Date

  def accepts(value: String): Boolean = this match
    case PostalCode  => ApiKnownFactType.postalCodePattern.matches(value)
    case CountryCode => ApiKnownFactType.countryCodePattern.matches(value)
    case Email       => ApiKnownFactType.emailPattern.matches(value)
    case Date        => ApiKnownFactType.datePattern.matches(value) && Try(LocalDate.parse(value)).isSuccess

object ApiKnownFactType:
  private val postalCodePattern: Regex = "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$|BFPO\\s?[0-9]{1,5}$".r
  private val countryCodePattern: Regex = "^[A-Z]{2}$".r
  private val emailPattern: Regex = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$".r
  private val datePattern: Regex = "^[0-9]{4}-[0-9]{2}-[0-9]{2}$".r

final class ApiKnownFact private (val value: String, val knownFactType: ApiKnownFactType)

object ApiKnownFact:
  def from(value: String): Option[ApiKnownFact] =
    ApiKnownFactType.values.find(_.accepts(value)).map(new ApiKnownFact(value, _))
