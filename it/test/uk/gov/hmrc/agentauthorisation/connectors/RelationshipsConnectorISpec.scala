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

package uk.gov.hmrc.agentauthorisation.connectors

import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Service.{HMRCMTDIT, HMRCMTDITSUPP}

import scala.concurrent.ExecutionContext.Implicits.global

class RelationshipsConnectorISpec extends BaseISpec {

  val connector: RelationshipsConnector = app.injector.instanceOf[RelationshipsConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "checkItsaRelationship" should {

    "return true when a relationship exists" in {
      getStatusRelationshipItsa(arn.value, validNino, 200, HMRCMTDIT)
      getStatusRelationshipItsa(arn.value, validNino, 404, HMRCMTDITSUPP)
      val result = await(connector.checkItsaRelationship(arn, validNino))
      result shouldBe true
    }

    "return false when a relationship is not found" in {
      getStatusRelationshipItsa(arn.value, validNino, 404, HMRCMTDIT)
      getStatusRelationshipItsa(arn.value, validNino, 404, HMRCMTDITSUPP)
      val result = await(connector.checkItsaRelationship(arn, validNino))
      result shouldBe false
    }

  }

  "checkVatRelationship" should {

    "return 204 when a relationship exists" in {
      getStatusRelationshipVat(arn.value, validVrn, 200)
      val result = await(connector.checkVatRelationship(arn, validVrn))
      result shouldBe true
    }

    "return 404 when a relationship is not found" in {
      getStatusRelationshipVat(arn.value, validVrn, 404)
      val result = await(connector.checkVatRelationship(arn, validVrn))
      result shouldBe false
    }

  }
}
