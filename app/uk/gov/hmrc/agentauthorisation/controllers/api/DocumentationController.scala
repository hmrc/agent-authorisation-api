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

import controllers.Assets
import javax.inject.{Inject, Singleton}
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.views.txt

case class ApiAccess(`type`: String)

object ApiAccess {
  implicit lazy val formats = Json.format[ApiAccess]
}

@Singleton
class DocumentationController @Inject() (
  errorHandler: HttpErrorHandler,
  appConfig: AppConfig,
  cc: ControllerComponents,
  assets: Assets
) extends uk.gov.hmrc.api.controllers.DocumentationController(cc, assets, errorHandler) {

  private val apiAccess = ApiAccess(appConfig.apiType)

  override def definition(): Action[AnyContent] = Action {
    Ok(txt.definition(apiAccess))
      .withHeaders("Content-Type" -> "application/json")
  }
}
