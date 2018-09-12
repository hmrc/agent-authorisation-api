package uk.gov.hmrc.agentauthorisation.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}

trait DesStubs {

  def givenMtdItIdIsKnownFor(nino: Nino, mtdbsa: MtdItId) =
    stubFor(
      get(urlEqualTo(s"/registration/business-details/nino/${nino.value}"))
        .willReturn(aResponse().withStatus(200).withBody(s"""{ "mtdbsa": "${mtdbsa.value}" }""")))

  def givenMtdItIdIsUnKnownFor(nino: Nino) =
    stubFor(
      get(urlEqualTo(s"/registration/business-details/nino/${nino.value}"))
        .willReturn(aResponse().withStatus(404)))

  def givenNinoIsInvalid(nino: Nino) =
    stubFor(
      get(urlMatching(s"/registration/.*?/nino/${nino.value}"))
        .willReturn(aResponse().withStatus(400)))

}
