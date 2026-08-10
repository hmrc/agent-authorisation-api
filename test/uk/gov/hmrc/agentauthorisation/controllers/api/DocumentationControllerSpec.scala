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

package uk.gov.hmrc.agentauthorisation.controllers.api

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HttpErrorHandler
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.support.BaseSpec
import controllers.Assets

class DocumentationControllerSpec extends BaseSpec with MockitoSugar:

  private def controller(accessType: String): DocumentationController =
    val appConfig = mock[AppConfig]
    when(appConfig.apiType).thenReturn(accessType)
    val controllerComponents = stubControllerComponents()
    val constructor = classOf[DocumentationController].getConstructors.head
    val arguments = constructor.getParameterTypes.toSeq.map:
      case cls if cls == classOf[AppConfig]            => appConfig
      case cls if cls == classOf[ControllerComponents] => controllerComponents
      case cls if cls == classOf[HttpErrorHandler]     => mock[HttpErrorHandler]
      case cls if cls == classOf[Assets]               => mock[Assets]
      case cls                                         => throw IllegalArgumentException(s"Unsupported constructor parameter: ${cls.getName}")

    constructor.newInstance(arguments.map(_.asInstanceOf[Object])*).asInstanceOf[DocumentationController]

  private def expectedDefinition(accessType: String) =
    Json.obj(
      "api" -> Json.obj(
        "name"        -> "Agent Authorisation",
        "description" -> "An API allowing MTD-enabled Agents to request authorisation to a service for a client, instead of filling the 64-8 paper form.",
        "context"     -> "agents",
        "versions" -> Json.arr(
          Json.obj(
            "version"          -> "1.0",
            "status"           -> "BETA",
            "endpointsEnabled" -> true,
            "access"           -> Json.obj("type" -> accessType)
          ),
          Json.obj(
            "version"          -> "2.0",
            "status"           -> "BETA",
            "endpointsEnabled" -> true,
            "access"           -> Json.obj("type" -> accessType)
          )
        )
      )
    )

  "definition" should:
    "return 200 JSON with the expected API definition for PRIVATE access" in:
      val result = controller("PRIVATE").definition()(FakeRequest()).futureValue

      status(result) shouldBe 200
      contentAsJson(result) shouldBe expectedDefinition("PRIVATE")

    "reflect the configured api access type" in:
      val result = controller("PUBLIC").definition()(FakeRequest()).futureValue
      val versions = (contentAsJson(result) \ "api" \ "versions").as[JsArray].value

      versions.map(_ \ "access" \ "type").map(_.as[String]).distinct shouldBe Seq("PUBLIC")
