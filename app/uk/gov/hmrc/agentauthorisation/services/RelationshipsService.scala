/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{ Inject, Singleton }
import uk.gov.hmrc.agentauthorisation.connectors.RelationshipsConnector
import uk.gov.hmrc.agentmtdidentifiers.model.{ Arn, MtdItId, Vrn }
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class RelationshipsService @Inject() (
  relationshipsConnector: RelationshipsConnector) {

  def checkItsaRelationshipsService(arn: Arn, mtdItId: MtdItId)(implicit headerCarrier: HeaderCarrier, executionContext: ExecutionContext): Future[Int] = {
    relationshipsConnector.checkItsaRelationship(arn, mtdItId).map(_.getOrElse(throw new Exception("Invitation location expected but missing.")))
  }

  def checkVatRelationshipService(arn: Arn, vrn: Vrn)(implicit headerCarrier: HeaderCarrier, executionContext: ExecutionContext): Future[Int] = {
    relationshipsConnector.checkVatRelationship(arn, vrn).map(_.getOrElse(throw new Exception("Invitation location expected but missing.")))
  }
}
