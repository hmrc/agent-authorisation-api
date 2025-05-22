/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.util

import play.api.mvc.{Request, RequestHeader}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider

import javax.inject.Inject

/** I'm repeating a pattern which was brought originally by play-framework and putting some more data which can be
  * derived from a request
  *
  * Use it to provide HeaderCarrier, Lang, or Messages
  */
class RequestSupport @Inject() () {
  implicit def hc(implicit request: Request[_]): HeaderCarrier = RequestSupport.hc
}

object RequestSupport {

  implicit def hc(implicit request: RequestHeader): HeaderCarrier = HcProvider.headerCarrier

  /** This is because we want to give responsibility of creation of HeaderCarrier to the platform code. If they refactor
    * how hc is created our code will pick it up automatically.
    */
  private object HcProvider extends BackendHeaderCarrierProvider {
    def headerCarrier(implicit request: RequestHeader): HeaderCarrier = hc(request)
  }

}
