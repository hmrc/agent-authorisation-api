/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.utils.UriEncoding
import uk.gov.hmrc.agentauthorisation.controllers.api.ApiTestSupport.Endpoint
import uk.gov.hmrc.agentauthorisation.support.Resource
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.xml.{Elem, XML}

object ApiTestSupport {

  case class Endpoint(uriPattern: String, endPointName: String, version: String)

}

abstract class ApiTestSupport(implicit ws: WSClient, hc: HeaderCarrier, ec: ExecutionContext) {

  val runningPort: Int

  private val definitionPath = "/api/definition"
  private val xmlDocumentationPath = "/api/documentation"
  private val yamlPath = "/api/conf"

  private def definitionsJson = new Resource(definitionPath.toString, runningPort).get().json

  private val DefinitionsFileApiSection = (definitionsJson \ "api").as[JsValue]
  private val DefinitionsFileApiVersions: List[JsValue] = (DefinitionsFileApiSection \ "versions").as[List[JsValue]]

  def xmlDocumentationFor(endpoint: Endpoint): (Int, Try[Elem]) = {
    val endpointPath = s"${endpoint.version}/${UriEncoding.encodePathSegment(endpoint.endPointName, "UTF-8")}"
    val response: HttpResponse = new Resource(s"$xmlDocumentationPath/$endpointPath", runningPort).get()
    (response.status, Try(XML.loadString(response.body)))
  }

  def yamlByVersion(api: JsValue): (String, String) = {
    val (apiVersion: String, response: HttpResponse) = yamlResponseByVersion(api)
    require(response.status == 200)
    apiVersion -> response.body
  }

  def yamlResponseByVersion(api: JsValue): (String, HttpResponse) = {
    val apiVersion: String = (api \ "version").as[String]
    val response: HttpResponse = new Resource(s"$yamlPath/$apiVersion/application.yaml", runningPort).get()
    apiVersion -> response
  }

  def forAllApiVersions[T](generator: (JsValue) => T, versions: List[JsValue] = DefinitionsFileApiVersions)(
    fn: T => Unit): Unit =
    versions.foreach(version => fn(generator(version)))
}
