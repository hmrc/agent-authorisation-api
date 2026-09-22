/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.http.HeaderNames
import play.api.mvc.{ActionTransformer, ControllerComponents, Request, WrappedRequest}
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models.ApiVersion

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

final class ApiVersionRequest[A](val apiVersion: ApiVersion, request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class ApiVersionAction @Inject() (appConfig: AppConfig, controllerComponents: ControllerComponents)
extends ActionTransformer[Request, ApiVersionRequest]:

  override protected def transform[A](request: Request[A]): Future[ApiVersionRequest[A]] =
    Future.successful(
      new ApiVersionRequest(
        ApiVersion.resolve(appConfig.v3Enabled, request.headers.get(HeaderNames.ACCEPT)),
        request
      )
    )

  override protected def executionContext: ExecutionContext = controllerComponents.executionContext
