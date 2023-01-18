package uk.gov.hmrc.agentauthorisation.auth

import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionsISpec extends BaseISpec {

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    implicit val hc = HeaderCarrier(authorization = Some(Authorization("Bearer XYZ")))
    implicit val request = FakeRequest().withHeaders("Authorization" -> "Bearer XYZ")

    def withAuthorisedAsAgent[A]: Result =
      await(super.withAuthorisedAsAgent { arn =>
        Future.successful(Ok(arn.value))
      })
  }

  "withAuthorisedAsAgent" should {

    "call body with arn and isAllowlisted flag when valid agent" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"affinityGroup":"Agent",
           |"allEnrolments": [
           |  { "key":"HMRC-AS-AGENT", "identifiers": [
           |    { "key":"AgentReferenceNumber", "value": "fooArn" }
           |  ]}
           |]}""".stripMargin
      )
      val result = TestController.withAuthorisedAsAgent
      status(result) shouldBe 200
      bodyOf(result) shouldBe "fooArn"
    }

    "return 401 UNAUTHORISED when no Bearer Token supplied" in {
      givenUnauthorisedWith("MissingBearerToken")
      val result = TestController.withAuthorisedAsAgent
      status(result) shouldBe 401
    }

    "return 403 FORBIDDEN when having InsufficientEnrolments" in {
      givenUnauthorisedWith("InsufficientEnrolments")
      val result = TestController.withAuthorisedAsAgent
      status(result) shouldBe 403
      result shouldBe NotAnAgent
    }

    "return 403 Not An Agent when agent not enrolled for service" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"affinityGroup":"Individual",
           |"allEnrolments": [
           |  { "key":"HMRC-MTD-IT", "identifiers": [
           |    { "key":"MTDITID", "value": "fooMtdItId" }
           |  ]}
           |]}""".stripMargin
      )
      val result = TestController.withAuthorisedAsAgent
      result shouldBe NotAnAgent
    }

    "return 403 Agent Not Subscribed when expected agent's identifier missing" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"affinityGroup":"Agent",
           |"allEnrolments": [
           |  { "key":"IR-SA", "identifiers": [
           |    { "key":"BAR", "value": "fooArn" }
           |  ]}
           |]}""".stripMargin
      )
      val result = TestController.withAuthorisedAsAgent
      result shouldBe AgentNotSubscribed
    }
  }
}
