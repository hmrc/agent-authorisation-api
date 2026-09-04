/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.support

import java.net.URI

import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RequestTarget
import play.api.mvc.Headers
import play.api.mvc.Request

object NoRequest extends Request[Any] {

  override def body: Any        = ""
  override def method: String   = ""
  override def version: String  = ""
  override def headers: Headers = Headers.create()

  override def connection: RemoteConnection = RemoteConnection(
    "",
    false,
    None
  )

  override def target: RequestTarget =
    new RequestTarget {
      override def uri: URI = URI.create("")

      override def uriString: String = ""

      override def path: String = ""

      override def queryMap: Map[String, Seq[String]] = Map.empty
    }

  override def attrs: TypedMap = TypedMap.empty

}
