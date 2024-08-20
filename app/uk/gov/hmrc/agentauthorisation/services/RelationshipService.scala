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

package uk.gov.hmrc.agentauthorisation.services

import uk.gov.hmrc.agentauthorisation.connectors.RelationshipsConnector
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Service => mtdServie, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.agentmtdidentifiers.model.{Service => MtdServie}

class RelationshipService @Inject() (relationshipsConnector: RelationshipsConnector) {

  def hasActiveRelationship(arn: Arn, clientId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): PartialFunction[MtdServie, Future[Boolean]] = {
    case mtdServie.MtdIt     => relationshipsConnector.checkItsaRelationship(arn, Nino(clientId))
    case mtdServie.MtdItSupp => relationshipsConnector.checkItsaSuppRelationship(arn, Nino(clientId))
    case mtdServie.Vat       => relationshipsConnector.checkVatRelationship(arn, Vrn(clientId))
  }

}
