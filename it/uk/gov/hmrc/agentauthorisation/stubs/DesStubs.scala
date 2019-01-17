package uk.gov.hmrc.agentauthorisation.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}

trait DesStubs {

  def givenMtdItIdIsKnownFor(nino: Nino, mtdbsa: MtdItId): StubMapping =
    stubFor(
      get(urlEqualTo(s"/registration/business-details/nino/${nino.value}"))
        .willReturn(aResponse().withStatus(200).withBody(s"""{ "mtdbsa": "${mtdbsa.value}" }""")))

  def givenMtdItIdIsUnKnownFor(nino: Nino): StubMapping =
    stubFor(
      get(urlEqualTo(s"/registration/business-details/nino/${nino.value}"))
        .willReturn(aResponse().withStatus(404)))

}
