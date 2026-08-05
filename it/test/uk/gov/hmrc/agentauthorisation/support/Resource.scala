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

package uk.gov.hmrc.agentauthorisation.support

import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.ws.DefaultBodyWritables.{writeableOf_String, writeableOf_WsBody}
import play.api.libs.ws.{EmptyBody, WSClient, WSRequest, WSResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.duration.{Duration, SECONDS, _}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

object Http {

  def get(url: String)(using hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse = perform(url) {
    request =>
      request.get()
  }

  def post(url: String, body: String, headers: Seq[(String, String)] = Seq.empty)(using
    hc: HeaderCarrier,
    ec: ExecutionContext,
    ws: WSClient
  ): HttpResponse = perform(url) { request =>
    request.addHttpHeaders(headers*).post(body)
  }

  def postEmpty(url: String)(using hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse =
    perform(url) { request =>
      request.post(EmptyBody)
    }

  def putEmpty(url: String)(using hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse =
    perform(url) { request =>
      request.put(EmptyBody)
    }

  def delete(url: String)(using hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse = perform(url) {
    request =>
      request.delete()
  }

  private def perform(
    url: String
  )(fun: WSRequest => Future[WSResponse])(using hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient) =
    await(
      fun(
        ws.url(url)
          .addHttpHeaders(hc.headersForUrl(HeaderCarrier.Config())(url)*)
          .withRequestTimeout(20000 milliseconds)
      ).map(response => HttpResponse(response.status, response.body))
    )

  private def await[A](future: Future[A]) = Await.result(future, Duration(10, SECONDS))

}

class Resource(path: String, port: Int) {

  def get()(using hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) =
    Http.get(s"http://localhost:$port$path")

  def postAsJson(body: String)(using hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) =
    Http.post(s"http://localhost:$port$path", body, Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))

  def postEmpty()(using hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) =
    Http.postEmpty(s"http://localhost:$port$path")

  def putEmpty()(using hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) =
    Http.putEmpty(s"http://localhost:$port$path")
}
