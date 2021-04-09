package uk.gov.hmrc.agentauthorisation.support

import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.ws.{EmptyBody, WSClient, WSRequest, WSResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.ws.WSHttpResponse

import scala.concurrent.duration.{Duration, SECONDS, _}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

object Http {

  def get(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse = perform(url) {
    request =>
      request.get()
  }

  def post(url: String, body: String, headers: Seq[(String, String)] = Seq.empty)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    ws: WSClient): HttpResponse = perform(url) { request =>
    request.addHttpHeaders(headers: _*).post(body)
  }

  def postEmpty(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse =
    perform(url) { request =>
      request.post(EmptyBody)
    }

  def putEmpty(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse =
    perform(url) { request =>

      request.put(EmptyBody)
    }

  def delete(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse = perform(url) {
    request =>
      request.delete()
  }

  private def perform(url: String)(fun: WSRequest => Future[WSResponse])(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    ws: WSClient) =
    await(
      fun(ws.url(url).addHttpHeaders(hc.headers: _*).withRequestTimeout(20000 milliseconds)).map(WSHttpResponse(_)))

  private def await[A](future: Future[A]) = Await.result(future, Duration(10, SECONDS))

}

class Resource(path: String, port: Int) {

  private def url() = s"http://localhost:$port$path"

  def get()(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) =
    Http.get(url)(hc, ec, ws)

  def postAsJson(body: String)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) =
    Http.post(url, body, Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))(hc, ec, ws)

  def postEmpty()(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) =
    Http.postEmpty(url)(hc, ec, ws)

  def putEmpty()(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) =
    Http.putEmpty(url)(hc, ec, ws)
}
