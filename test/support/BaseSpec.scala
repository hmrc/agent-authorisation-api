/*
 * Copyright 2021 HM Revenue & Customs
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

import java.net.URI

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.{RemoteConnection, RequestTarget}
import play.api.mvc.{Headers, RequestHeader}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

abstract class BaseSpec extends UnitSpec {
  implicit val sys: ActorSystem = ActorSystem("TestSystem")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val arn: Arn = Arn("TARN0000001")

  def testRequest[A](fakeRequest: FakeRequest[A]): RequestHeader = new RequestHeader {
    override def method: String = fakeRequest.method
    override def version: String = fakeRequest.version
    override def headers: Headers = fakeRequest.headers

    override def connection: RemoteConnection = RemoteConnection("", false, None)

    override def target: RequestTarget = new RequestTarget {
      override def uri: URI = URI.create("")

      override def uriString: String = fakeRequest.uri

      override def path: String = fakeRequest.path

      override def queryMap: Map[String, Seq[String]] = fakeRequest.queryString
    }

    override def attrs: TypedMap = TypedMap.empty
  }
}
