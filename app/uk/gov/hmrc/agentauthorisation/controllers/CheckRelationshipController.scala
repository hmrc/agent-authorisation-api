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

package uk.gov.hmrc.agentauthorisation.controllers

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.agentauthorisation.actions.{ApiVersionAction, AuthorisedAgentAction}
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.services.{CheckRelationshipService, ValidateClientAccessDataService}
import uk.gov.hmrc.agentauthorisation.models.Arn
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckRelationshipController @Inject() (
  checkRelationshipService: CheckRelationshipService,
  validateClientAccessDataService: ValidateClientAccessDataService,
  cc: ControllerComponents,
  apiVersionAction: ApiVersionAction,
  authorisedAgentAction: AuthorisedAgentAction
)(using val ec: ExecutionContext)
    extends BackendController(cc):

  def checkRelationship(givenArn: Arn): Action[AnyContent] =
    cc.actionBuilder.andThen(apiVersionAction).andThen(authorisedAgentAction(givenArn)).async { request =>
      validateClientAccessDataService
        .validateCheckRelationshipPayload(request.body.asJson)
        .fold(
          errorResponse => {
            Logger(getClass).warn(s"Payload failed validation: $errorResponse")
            Future.successful(errorResponse.toResult)
          },
          clientAccessData =>
            checkRelationshipService.checkRelationship(request.arn, clientAccessData)(using request).map {
              case Right(false) =>
                RelationshipNotFound.toResult
              case Right(true) =>
                NoContent
              case Left(errorResponse: ApiErrorResponse) =>
                errorResponse.toResult
            }
        )
    }
