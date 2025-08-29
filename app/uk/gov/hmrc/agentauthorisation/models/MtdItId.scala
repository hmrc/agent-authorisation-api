/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.domain.{SimpleObjectReads, SimpleObjectWrites, TaxIdentifier}

case class MtdItId(value: String) extends TaxIdentifier

object MtdItId {

  private val pattern = "^[0-9A-Za-z]{1,15}$".r

  def isValid(mtdItId: String): Boolean =
    mtdItId match {
      case pattern(_*) => true
      case _           => false
    }

  implicit val reads: SimpleObjectReads[MtdItId] = new SimpleObjectReads[MtdItId]("value", MtdItId.apply)
  implicit val writes: SimpleObjectWrites[MtdItId] = new SimpleObjectWrites[MtdItId](_.value)

}
