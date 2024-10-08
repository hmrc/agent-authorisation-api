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

package uk.gov.hmrc.agentauthorisation.models

import play.api.libs.json._

sealed trait AgentType {
  def agentTypeName: String
}

object AgentType {

  def apply(name: String): AgentType =
    name match {
      case "main"       => Main
      case "supporting" => Supporting
    }

  final case object Main extends AgentType {
    override val agentTypeName: String = "main"
  }

  final case object Supporting extends AgentType {
    override val agentTypeName: String = "supporting"
  }

  implicit val writes: Writes[AgentType] = new Writes[AgentType] {
    override def writes(agentType: AgentType): JsValue = JsString(agentType.agentTypeName)
  }

}
