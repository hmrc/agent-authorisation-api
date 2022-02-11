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

package uk.gov.hmrc.agentauthorisation.support

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.test.Helpers
import play.api.test.Helpers.defaultAwaitTimeout

import scala.concurrent.Future

trait UnitSpec extends AnyWordSpecLike with Matchers with OptionValues with ScalaFutures {
  // the following is a collection of useful methods that should minimise
  // the changes required when migrating away from hmrctest, which is now deprecated.
  def status(result: Result): Int = result.header.status

  def status(result: Future[Result]): Int = Helpers.status(result)

  def bodyOf(result: Result): String = Helpers.contentAsString(Future.successful(result))

  def redirectLocation(result: Result) = Helpers.redirectLocation(Future.successful(result))

  def contentAsString(result: Result): String = Helpers.contentAsString(Future.successful(result))

  def contentAsJson(result: Result): JsValue = Helpers.contentAsJson(Future.successful(result))

  def contentType(result: Result): Option[String] =
    result.body.contentType.map(_.split(";").take(1).mkString.trim)

  def charset(result: Result): Option[String] =
    result.body.contentType match {
      case Some(s) if s.contains("charset=") => Some(s.split("; *charset=").drop(1).mkString.trim)
      case _                                 => None
    }
}
