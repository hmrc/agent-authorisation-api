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

package uk.gov.hmrc.agentauthorisation

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.http.Status._
import play.api.mvc._
import uk.gov.hmrc.agentauthorisation.controllers.api.errors.ErrorResponse._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.http.JsonErrorHandler
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject()(auditConnector: AuditConnector, httpAuditEvent: HttpAuditEvent)(
  implicit ec: ExecutionContext,
  configuration: Configuration)
    extends JsonErrorHandler(auditConnector, httpAuditEvent, configuration) {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    super.onClientError(request, statusCode, message).map { auditedError =>
      Logger(getClass).warn(s"Client Side Error: $message from request: $request statusCode: $statusCode")
      statusCode match {
        case NOT_FOUND    => standardNotFound
        case BAD_REQUEST  => standardBadRequest
        case UNAUTHORIZED => standardUnauthorised
        case _            => auditedError
      }
    }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
    super
      .onServerError(request, exception)
      .map(_ => {
        Logger(getClass).warn(s"Server Side Error: $exception from request: $request")
        standardInternalServerError
      })
}
