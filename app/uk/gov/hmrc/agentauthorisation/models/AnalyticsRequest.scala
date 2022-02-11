/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.{Json, OWrites}

case class DimensionValue(index: Int, value: String)

object DimensionValue {
  implicit val dimensionWrites: OWrites[DimensionValue] = Json.writes[DimensionValue]
}

case class Event(category: String, action: String, label: String, dimensions: Seq[DimensionValue])

object Event {
  implicit val eventWrites: OWrites[Event] = Json.writes[Event]
}

case class AnalyticsRequest(gaClientId: Option[String], gaTrackingId: Option[String], events: List[Event])

object AnalyticsRequest {
  implicit val analyticsWrites: OWrites[AnalyticsRequest] = Json.writes[AnalyticsRequest]
}
