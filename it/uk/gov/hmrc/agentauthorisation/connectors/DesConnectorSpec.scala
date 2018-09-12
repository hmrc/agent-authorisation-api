package uk.gov.hmrc.agentauthorisation.connectors

import com.kenshoo.play.metrics.Metrics
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Utr, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext

class DesConnectorSpec extends BaseISpec {

  override implicit lazy val app: Application = appBuilder
    .build()

  val httpGet = app.injector.instanceOf[HttpGet]
  val httpPost = app.injector.instanceOf[HttpPost]

  private implicit val hc = HeaderCarrier()
  private implicit val ec = ExecutionContext.global

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
      await(desConnector.getMtdIdFor(nino)) shouldBe mtdItId
    }

    "return nothing when agent's nino identifier is unknown to ETMP" in {
      givenMtdItIdIsUnKnownFor(nino)
      an[Exception] should be thrownBy await(desConnector.getMtdIdFor(nino))
    }
  }
}
