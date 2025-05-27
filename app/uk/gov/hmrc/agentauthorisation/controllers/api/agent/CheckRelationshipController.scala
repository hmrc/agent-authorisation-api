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

package uk.gov.hmrc.agentauthorisation.controllers.api.agent

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.agentauthorisation.auth.AuthActions
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.services.{CheckRelationshipService, ValidateClientAccessDataService}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckRelationshipController @Inject() (
  checkRelationshipService: CheckRelationshipService,
  validateClientAccessDataService: ValidateClientAccessDataService,
  val authConnector: AuthConnector,
  cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BackendController(cc) with AuthActions {

  def checkRelationship(givenArn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      implicit val loggedInArn: Arn = arn
      validateArnInRequest(givenArn) {
        validateClientAccessDataService
          .validateCheckRelationshipPayload(request.body.asJson)
          .fold(
            errorResponse => {
              Logger(getClass).warn(s"Payload failed validation: $errorResponse")
              Future successful errorResponse.toResult
            },
            clientAccessData =>
              for {
                result <- checkRelationshipService.checkRelationship(arn, clientAccessData)
              } yield result match {
                case Right(false) =>
                  RelationshipNotFound.toResult
                case Right(true) =>
                  NoContent
                case Left(errorResponse: ApiErrorResponse) =>
                  errorResponse.toResult
              }
          )
      }
    }
  }
}
