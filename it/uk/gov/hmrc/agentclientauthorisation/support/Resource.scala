package uk.gov.hmrc.agentclientauthorisation.support

import play.api.http.{ HeaderNames, MimeTypes }
import play.api.libs.ws.{ WS, WSClient, WSRequest, WSResponse }
import play.api.mvc.Results
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.http.ws.WSHttpResponse

import scala.concurrent.duration.{ Duration, SECONDS, _ }
import scala.concurrent.{ Await, ExecutionContext, Future }

object Http {

  def get(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse = perform(url) { request =>
    request.get()
  }

  def post(url: String, body: String, headers: Seq[(String, String)] = Seq.empty)(implicit hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse = perform(url) { request =>
    request.withHeaders(headers: _*).post(body)
  }

  def postEmpty(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse = perform(url) { request =>
    import play.api.http.Writeable._
    request.post(Results.EmptyContent())
  }

  def putEmpty(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse = perform(url) { request =>
    import play.api.http.Writeable._
    request.put(Results.EmptyContent())
  }

  def delete(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): HttpResponse = perform(url) { request =>
    request.delete()
  }

  private def perform(url: String)(fun: WSRequest => Future[WSResponse])(implicit hc: HeaderCarrier, ec: ExecutionContext, ws: WSClient): WSHttpResponse =
    await(fun(ws.url(url).withHeaders(hc.headers: _*).withRequestTimeout(20000 milliseconds)).map(new WSHttpResponse(_)))

  private def await[A](future: Future[A]) = Await.result(future, Duration(10, SECONDS))

}

class Resource(path: String, port: Int) {

  private def url() = s"http://localhost:$port$path"

  def get()(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) = Http.get(url)(hc, ec, ws)

  def postAsJson(body: String)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) =
    Http.post(url, body, Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))(hc, ec, ws)

  def postEmpty()(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) =
    Http.postEmpty(url)(hc, ec, ws)

  def putEmpty()(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext, ws: WSClient) =
    Http.putEmpty(url)(hc, ec, ws)
}