package uk.gov.hmrc.agentauthorisation.controllers.api

import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentauthorisation.auth.AuthActions
import uk.gov.hmrc.agentauthorisation.support.BaseISpec
import uk.gov.hmrc.auth.core.{ AuthConnector, AuthorisationException }
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.agentauthorisation.controllers.api.ErrorResults._

import scala.concurrent.Future

class AuthActionsISpec extends BaseISpec {

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    def withVerifiedPasscode: PasscodeVerification = app.injector.instanceOf[PasscodeVerification]

    implicit val hc = HeaderCarrier()
    implicit val request = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")

    def withAuthorisedAsAgent[A]: Result =
      await(super.withAuthorisedAsAgent { (arn, isWhitelisted) =>
        Future.successful(Ok((arn.value, isWhitelisted).toString))
      })
  }

  "withAuthorisedAsAgent" should {

    "call body with arn and isWhitelisted flag when valid agent" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-AS-AGENT", "identifiers": [
           |    { "key":"AgentReferenceNumber", "value": "fooArn" }
           |  ]}
           |]}""".stripMargin)
      val result = TestController.withAuthorisedAsAgent
      status(result) shouldBe 200
      bodyOf(result) shouldBe "(fooArn,false)"
    }

    "throw AuthorisationException when user not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      an[AuthorisationException] shouldBe thrownBy {
        TestController.withAuthorisedAsAgent
      }
    }

    "throw InsufficientEnrolments when agent not enrolled for service" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-MTD-IT", "identifiers": [
           |    { "key":"MTDITID", "value": "fooMtdItId" }
           |  ]}
           |]}""".stripMargin)
      val result = await(TestController.withAuthorisedAsAgent)
      result shouldBe NotAnAgent
    }

    "throw InsufficientEnrolments when expected agent's identifier missing" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-AS-AGENT", "identifiers": [
           |    { "key":"BAR", "value": "fooArn" }
           |  ]}
           |]}""".stripMargin)
      val result = await(TestController.withAuthorisedAsAgent)
      result shouldBe NotAnAgent
    }
  }
}
