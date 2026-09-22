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

package uk.gov.hmrc.agentauthorisation.actions

import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.support.BaseISpec

import scala.concurrent.Future

class AuthorisedAgentActionISpec extends BaseISpec:

  private lazy val action = app.injector.instanceOf[AuthorisedAgentAction]

  private def invoke(
    requestedArn: Arn = arn,
    apiVersion: ApiVersion = ApiVersion.V3
  ): Result =
    val request = FakeRequest().withHeaders(HeaderNames.AUTHORIZATION -> "Bearer XYZ")
    val versionedRequest = new ApiVersionRequest(apiVersion, request)

    action(requestedArn).invokeBlock(
      versionedRequest,
      authorisedRequest =>
        Future.successful(
          Ok(
            Json.obj(
              "arn"        -> authorisedRequest.arn.value,
              "apiVersion" -> authorisedRequest.apiVersion.toString
            )
          )
        )
    ).futureValue

  "AuthorisedAgentAction" should:
    "pass the authenticated ARN and selected API version to the next action" in:
      givenAuthorisedAsValidAgent(arn.value)

      val result = invoke()

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.obj(
        "arn"        -> arn.value,
        "apiVersion" -> "V3"
      )

    "return NO_PERMISSION_ON_AGENCY when the requested ARN differs from the authenticated ARN" in:
      givenAuthorisedAsValidAgent(arn2.value)

      val result = invoke()

      status(result) shouldBe 403
      contentAsJson(result) shouldBe NoPermissionOnAgency.toJson

    "return NOT_AN_AGENT for a non-agent user" in:
      givenAuthorisedFor(
        s"""
           |{
           |  "authorise": [
           |    { "authProviders": ["GovernmentGateway"] }
           |  ],
           |  "retrieve":["affinityGroup","allEnrolments"]
           |}
           """.stripMargin,
        s"""
           |{
           |"affinityGroup":"Individual",
           |"allEnrolments": []
           |}
           """.stripMargin
      )

      val result = invoke()

      status(result) shouldBe 403
      contentAsJson(result) shouldBe NotAnAgent.toJson

    "return AGENT_NOT_SUBSCRIBED when an agent has no HMRC-AS-AGENT enrolment" in:
      givenAuthenticatedAgent(Enrolment("IR-SA-AGENT", "IRAgentReference", "someIRAR"))

      val result = invoke()

      status(result) shouldBe 403
      contentAsJson(result) shouldBe AgentNotSubscribed.toJson

    "return NOT_AN_AGENT for insufficient enrolments" in:
      givenUnauthorisedWith("InsufficientEnrolments")

      val result = invoke()

      status(result) shouldBe 403
      contentAsJson(result) shouldBe NotAnAgent.toJson

    "return UNAUTHORIZED for any other authorisation failure" in:
      givenUnauthorisedWith("MissingBearerToken")

      val result = invoke()

      status(result) shouldBe 401
      contentAsJson(result) shouldBe StandardUnauthorised.toJson
