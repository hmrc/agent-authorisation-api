/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.controllers

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentauthorisation.models.{NoPermissionOnAgency, UnsupportedClientType}
import uk.gov.hmrc.agentauthorisation.support.{BaseISpec, Resource}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import scala.concurrent.ExecutionContext.Implicits.global

class LegacyDeauthoriseActionRoutingISpec extends BaseISpec:

  override protected def additionalConfiguration: Map[String, Any] =
    Map("features.enable-v3" -> true)

  private given WSClient = app.injector.instanceOf[WSClient]
  private given HeaderCarrier = HeaderCarrier(
    authorization = Some(Authorization("Bearer XYZ")),
    otherHeaders = Seq("Accept" -> "application/vnd.hmrc.2.0+json")
  )

  private val payload = Json.obj(
    "service"      -> Json.arr("MTD-IT"),
    "clientType"   -> "personal",
    "clientIdType" -> "ni",
    "clientId"     -> validNino.value
  )

  private def deauthorise(body: String) =
    new Resource(s"/agents/${arn.value}/deauthorise-client", port).putAsJson(body)

  "the routed legacy deauthorise PUT with V3 enabled" should:
    "still return 204 after removing the relationship" in:
      givenAuthorisedAsValidAgent(arn.value)
      givenRemoveAuthorisationStub(
        arn = arn,
        clientId = validNino.value,
        service = "HMRC-MTD-IT",
        status = 204
      )

      val response = deauthorise(payload.toString())

      response.status shouldBe 204
      response.body shouldBe empty

    "still reject a route ARN that differs from the authenticated ARN" in:
      givenAuthorisedAsValidAgent(arn2.value)

      val response = deauthorise(payload.toString())

      response.status shouldBe 403
      response.json shouldBe NoPermissionOnAgency.toJson

    "still validate the legacy request body" in:
      givenAuthorisedAsValidAgent(arn.value)
      val invalidPayload = payload + ("clientType" -> Json.toJson("business"))

      val response = deauthorise(invalidPayload.toString())

      response.status shouldBe 400
      response.json shouldBe UnsupportedClientType.toJson
