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

package uk.gov.hmrc.agentauthorisation.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentauthorisation.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.Vrn
import uk.gov.hmrc.domain.Nino

trait ACRStubs {
  me: WireMockSupport =>

  def getStatusRelationshipItsa(arn: String, nino: Nino, status: Int, service: String): Unit =
    stubFor(
      get(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/$service/client/NI/${nino.value}"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def getStatusRelationshipItsaSupporting(arn: String, nino: Nino, status: Int): Unit =
    stubFor(
      get(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-MTD-IT-SUPP/client/NI/${nino.value}"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def getStatusRelationshipVat(arn: String, vrn: Vrn, status: Int): Unit =
    stubFor(
      get(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-MTD-VAT/client/VRN/${vrn.value}"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

}
