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

import javax.inject.{Inject, Singleton}
import play.api.http.{ContentTypes, MimeTypes}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, Codec, ControllerComponents}
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.views.txt
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future

case class ApiAccess(`type`: String)

object ApiAccess:
  given OFormat[ApiAccess] = Json.format[ApiAccess]

@Singleton
class DocumentationController @Inject() (
  appConfig: AppConfig,
  cc: ControllerComponents
) extends BackendController(cc) {

  private val apiAccess = ApiAccess(appConfig.apiType)

  def definition(): Action[AnyContent] = Action.async:
    Future.successful(
      Ok(txt.definition(apiAccess))
        .as(ContentTypes.withCharset(MimeTypes.JSON)(using Codec.utf_8))
    )

  def documentation(version: String, endpointName: String): Action[AnyContent] = Action:
    Ok(
      <documentation version={version} endpoint={endpointName}>
        <summary>Static documentation placeholder retained for Scala 3 migration.</summary>
      </documentation>
    )
}
