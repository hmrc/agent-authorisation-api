package uk.gov.hmrc.agentauthorisation.connectors

import com.kenshoo.play.metrics.Metrics
import play.api.Application
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global

class DesConnectorSpec extends BaseISpec {

  override implicit lazy val app: Application = appBuilder
    .build()

  val httpGet = app.injector.instanceOf[HttpGet]
  val httpPost = app.injector.instanceOf[HttpPost]

  private implicit val hc = HeaderCarrier()

  val http = app.injector.instanceOf[HttpPost with HttpGet with HttpPut]

  val desConnector =
    new DesConnector(wireMockBaseUrl, "token", "stub", http, app.injector.instanceOf[Metrics])

  val mtdItId = MtdItId("ABCDEF123456789")
  val vrn = Vrn("101747641")
  val agentARN = Arn("ABCDE123456")

  "DesConnector getMtdIdFor" should {

    val mtdItId = MtdItId("foo")
    val nino = Nino("AB123456C")

    "return MtdItId when agent's nino is known to ETMP" in {
      givenMtdItIdIsKnownFor(nino, mtdItId)
      await(desConnector.getMtdIdFor(nino)) shouldBe Right(mtdItId)
    }

    "return nothing when agent's nino identifier is unknown to ETMP" in {
      givenMtdItIdIsUnKnownFor(nino)
      await(desConnector.getMtdIdFor(nino)) shouldBe Left(false)
    }
  }
}
