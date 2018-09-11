package uk.gov.hmrc.agentauthorisation.connectors

import uk.gov.hmrc.agentauthorisation._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class RelationshipsConnectorISpec extends BaseISpec {

  val connector: RelationshipsConnector = app.injector.instanceOf[RelationshipsConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "checkItsaRelationship" should {

    "return true when a relationship exists" in {
      getStatusRelationshipItsa(arn.value, mtdItId, 200)
      val result = await(connector.checkItsaRelationship(arn, mtdItId))
      result shouldBe true
    }

    "return false when a relationship is not found" in {
      getStatusRelationshipItsa(arn.value, mtdItId, 404)
      val result = await(connector.checkItsaRelationship(arn, mtdItId))
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
