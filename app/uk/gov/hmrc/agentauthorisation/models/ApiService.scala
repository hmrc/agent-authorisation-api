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

package uk.gov.hmrc.agentauthorisation.models

import scala.util.matching.Regex

enum ApiService(val value: String, private val clientIdPatterns: Seq[Regex]):
  case Pillar2 extends ApiService("PILLAR2", Seq("^X[A-Z]PLR[0-9]{10}$".r))
  case Cbc extends ApiService("CBC", Seq("^X[A-Z]CBC[0-9]{10}$".r))
  case CgtPd extends ApiService("CGT-PD", Seq("^X[A-Z]CGTP[0-9]{9}$".r))
  case Trusts extends ApiService("TRUSTS", Seq("^[0-9]{10}$".r, "^[A-Z]{2}TRUST[0-9]{8}$".r))
  case Irv extends ApiService("IRV", Seq(ApiService.ninoPattern))
  case Ppt extends ApiService("PPT", Seq("^X[A-Z]PPT000[0-9]{7}$".r))
  case MtdVat extends ApiService("MTD-VAT", Seq("^[0-9]{9}$".r))
  case MtdIt extends ApiService("MTD-IT", Seq(ApiService.ninoPattern))

  def accepts(clientId: ApiClientId): Boolean =
    clientIdPatterns.exists(_.matches(clientId.value))

  private[models] def acceptsRaw(value: String): Boolean =
    clientIdPatterns.exists(_.matches(value))

object ApiService:
  private def ninoPattern = "[[A-Z]&&[^DFIQUV]][[A-Z]&&[^DFIQUVO]] ?[0-9]{2} ?[0-9]{2} ?[0-9]{2} ?[A-D]".r

  def from(value: String): Option[ApiService] =
    values.find(_.value == value)

opaque type ApiClientId = String

object ApiClientId:
  def from(value: String): Option[ApiClientId] =
    Option.when(ApiService.values.exists(_.acceptsRaw(value)))(value)

  extension (clientId: ApiClientId) def value: String = clientId

final class ApiServiceClient private (val service: ApiService, val clientId: ApiClientId)

object ApiServiceClient:
  def validate(service: String, clientId: String): Either[ApiErrorResponse, ApiServiceClient] =
    for
      parsedService <- ApiService.from(service).toRight(UnsupportedService)
      parsedClientId <- ApiClientId.from(clientId).toRight(ClientIdInvalidFormat)
      compatiblePair <- from(parsedService, parsedClientId)
    yield compatiblePair

  def from(service: ApiService, clientId: ApiClientId): Either[ApiErrorResponse, ApiServiceClient] =
    Either.cond(
      service.accepts(clientId),
      new ApiServiceClient(service, clientId),
      ClientIdIncompatibleWithService
    )
