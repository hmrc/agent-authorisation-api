package uk.gov.hmrc.agentauthorisation.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentauthorisation.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.{ MtdItId, Vrn }

trait ACRStubs {
  me: WireMockSupport =>

  def getStatusRelationshipItsa(arn: String, mtdItId: MtdItId, status: Int): Unit = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-MTD-IT/client/MTDITID/${mtdItId.value}"))
      .willReturn(
        aResponse()
          .withStatus(status)))
  }

  def getStatusRelationshipVat(arn: String, vrn: Vrn, status: Int): Unit = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-MTD-VAT/client/VRN/${vrn.value}"))
      .willReturn(
        aResponse()
          .withStatus(status)))
  }

}
