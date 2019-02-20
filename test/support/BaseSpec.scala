/*
 * Copyright 2019 HM Revenue & Customs
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

package support

import java.security.cert.X509Certificate

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.{Headers, RequestHeader}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.api.connector.ServiceLocatorConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

abstract class BaseSpec extends UnitSpec with MockitoSugar with OneAppPerSuite {
  implicit val sys: ActorSystem = ActorSystem("TestSystem")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val arn: Arn = Arn("TARN0000001")

  val mockServiceLocator: ServiceLocatorConnector = mock[ServiceLocatorConnector]

  when(mockServiceLocator.register).thenReturn(Future.successful(true))

  def testRequest[A](fakeRequest: FakeRequest[A]): RequestHeader = new RequestHeader {
    override def id: Long = fakeRequest.id
    override def tags: Map[String, String] = fakeRequest.tags
    override def uri: String = fakeRequest.uri
    override def path: String = fakeRequest.path
    override def method: String = fakeRequest.method
    override def version: String = fakeRequest.version
    override def queryString: Map[String, Seq[String]] = fakeRequest.queryString
    override def headers: Headers = fakeRequest.headers
    override def remoteAddress: String = fakeRequest.remoteAddress
    override def secure: Boolean = fakeRequest.secure
    override def clientCertificateChain: Option[Seq[X509Certificate]] = fakeRequest.clientCertificateChain
  }
}
