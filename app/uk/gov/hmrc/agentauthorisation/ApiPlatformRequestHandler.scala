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

package uk.gov.hmrc.agentauthorisation

import javax.inject.Inject
import play.api.http.{
  DefaultHttpRequestHandler,
  HttpConfiguration,
  HttpErrorHandler,
  HttpFilters
}
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router

/**
  * Normalise the request path. The API platform strips the context
  * '/agents' from the URL before forwarding the request.
  * Re-add it here if necessary.
  */
class ApiPlatformRequestHandler @Inject()(router: Router,
                                          errorHandler: HttpErrorHandler,
                                          configuration: HttpConfiguration,
                                          filters: HttpFilters)
    extends DefaultHttpRequestHandler(router,
                                      errorHandler,
                                      configuration,
                                      filters) {

  val context = "/agents"
  val health = "/ping"
  val api = "/api"

  override def handlerForRequest(
      request: RequestHeader): (RequestHeader, Handler) = {
    if (request.path.startsWith(health)) {
      super.handlerForRequest(request)
    } else if (request.path.startsWith(api)) {
      super.handlerForRequest(request)
    } else if (!request.path.startsWith(context)) {
      super.handlerForRequest(request.copy(path = context + request.path))
    } else {
      super.handlerForRequest(request)
    }
  }

}
