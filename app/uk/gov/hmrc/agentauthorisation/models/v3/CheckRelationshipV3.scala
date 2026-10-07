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

import play.api.libs.json.{Json, OWrites}

enum RelationshipAgentType(val value: String):
  case Main extends RelationshipAgentType("main")
  case Supporting extends RelationshipAgentType("supporting")
  case MtdSignup extends RelationshipAgentType("mtdSignup")

final case class CheckRelationshipV3Response(
  agentAuthorised: Boolean,
  userAssigned: Option[Boolean] = None,
  agentType: Option[RelationshipAgentType] = None
)

object CheckRelationshipV3Response:
  val NotAuthorised: CheckRelationshipV3Response = CheckRelationshipV3Response(agentAuthorised = false)

  def authorised(userAssigned: Boolean, agentType: Option[RelationshipAgentType] = None): CheckRelationshipV3Response =
    CheckRelationshipV3Response(agentAuthorised = true, Some(userAssigned), agentType)

  given OWrites[CheckRelationshipV3Response] = response =>
    Json.obj("agentAuthorised" -> response.agentAuthorised) ++
      response.userAssigned.fold(Json.obj())(value => Json.obj("userAssigned" -> value)) ++
      response.agentType.fold(Json.obj())(value => Json.obj("agentType" -> value.value))
