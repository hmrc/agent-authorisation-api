/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.controllers.api.errors.ErrorResponse.{errorAcceptHeaderInvalidCustomMessage, errorBadRequestCustomMessage}

import scala.concurrent.Future
import scala.util.Try
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

@Singleton
class AcceptHeaderFilter @Inject()(appConfig: AppConfig)(implicit materializer: Materializer) extends Filter {
  override implicit def mat: Materializer = materializer

  import AcceptHeaderFilter._

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {

    def getGroup(key: String)(matches: Match): Option[String] = Try(matches.group(key)).toOption

    val uriExclusions = Seq("\\/ping\\/ping", "\\/api.+")
    val excludedRequestUri: Boolean = uriExclusions.exists(rh.uri.matches)

    val acceptHeader: Option[String] = rh.headers.get(HeaderNames.ACCEPT)
    val optMatch: Option[Match] = acceptHeader.flatMap(acceptHeaderRegex.findFirstMatchIn)

    val acceptType = optMatch.flatMap(getGroup("accept-type"))
    val isSupportedVersion = optMatch.flatMap(getGroup("version")).fold(false)(appConfig.apiSupportedVersions.contains)
    val isSupportedContentType =
      optMatch.flatMap(getGroup("content-type")).fold(false)(c => c.equalsIgnoreCase("json"))

    val errorMessage =
      if (excludedRequestUri) None
      else {
        (acceptHeader, acceptType, isSupportedVersion, isSupportedContentType) match {
          case (None, _, _, _)  => Some(errorAcceptHeaderInvalidCustomMessage("Missing 'Accept' header."))
          case (_, None, _, _)  => Some(errorAcceptHeaderInvalidCustomMessage("Invalid 'Accept' header."))
          case (_, _, false, _) => Some(errorBadRequestCustomMessage("Missing or unsupported version number."))
          case (_, _, _, false) => Some(errorBadRequestCustomMessage("Missing or unsupported content-type."))
          case _                => None
        }
      }

    errorMessage match {
      case Some(e) => Future.successful(e)
      case _       => f(rh)
    }
  }
}

object AcceptHeaderFilter {
  val acceptHeaderRegex: Regex =
    new Regex("""^(application/vnd\.hmrc)\.(.*)\+(.*)$""", "accept-type", "version", "content-type")
}
