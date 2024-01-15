/*
 * Copyright 2023 HM Revenue & Customs
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

import akka.stream.Materializer
import controllers.Assets
import play.api.Configuration
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.filters.cors.CORSActionBuilder
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class YamlController @Inject() (assets: Assets, configuration: Configuration, cc: ControllerComponents)(implicit
  mat: Materializer,
  ec: ExecutionContext
) extends BackendController(cc) {

  def yaml(version: String, file: String): Action[AnyContent] =
    CORSActionBuilder(configuration).async { implicit request =>
      assets.at(s"/public/api/conf/$version", file)(request)
    }
}
