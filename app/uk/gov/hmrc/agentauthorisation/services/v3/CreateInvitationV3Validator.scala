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

package uk.gov.hmrc.agentauthorisation.services.v3

import play.api.libs.json.JsValue
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.models.v3.*

object CreateInvitationV3Validator:
  def validate(payload: Option[JsValue]): Either[ApiErrorResponse, ValidatedCreateInvitationV3] =
    for
      request <- CreateInvitationV3Request.parse(payload)
      service <- request.resolveService
      clientId <- request.resolveClientId
      _ <- Either.cond(service.accepts(clientId), (), ClientIdNotCompatibleWithService)
      _ <- request.validateKnownFactPresence(service)
      knownFact <- request.resolveKnownFact
      configuration = ApiServiceConfiguration.forCommand(service, clientId)
      _ <- Either.cond(
        knownFact.forall(fact => configuration.acceptedKnownFactTypes.contains(fact.knownFactType)),
        (),
        KnownFactIncompatibleWithService
      )
      agentType <- request.resolveAgentType
    yield ValidatedCreateInvitationV3(service, clientId, knownFact, agentType)
