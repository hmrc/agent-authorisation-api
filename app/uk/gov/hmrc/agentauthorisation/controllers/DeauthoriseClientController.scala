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

package uk.gov.hmrc.agentauthorisation.controllers

import play.api.Logger
import play.api.mvc.*
import uk.gov.hmrc.agentauthorisation.actions.VersionedAgentAction
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models.*
import uk.gov.hmrc.agentauthorisation.services.v3.DeauthoriseRelationshipV3Service
import uk.gov.hmrc.agentauthorisation.services.{DeleteRelationshipService, ValidateClientAccessDataService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeauthoriseClientController @Inject() (
  deleteRelationshipService: DeleteRelationshipService,
  deauthoriseRelationshipV3Service: DeauthoriseRelationshipV3Service,
  validateClientAccessDataService: ValidateClientAccessDataService,
  versionedAgentAction: VersionedAgentAction,
  appConfig: AppConfig,
  cc: ControllerComponents
)(using val ec: ExecutionContext)
    extends BackendController(cc):

  def deauthoriseRelationshipV1V2(givenArn: Arn): Action[AnyContent] =
    versionedAgentAction(givenArn)(
      v3 = _ => Future.successful(NotAcceptable("invalid client version, this endpoint requires one of [V1,V2]")),
      v1V2 = request =>
        val validatedPayload = validateClientAccessDataService.validateDeleteRelationshipPayload(request.body.asJson)

        validatedPayload match
          case Left(errorResponse) =>
            Logger(getClass).warn(s"Payload failed validation: $errorResponse")
            Future.successful(errorResponse.toResult)

          case Right(payload) =>
            deleteRelationshipService
              .deleteRelationship(request.arn, payload)(using request)
              .map:
                case Right(_)            => NoContent
                case Left(errorResponse) => errorResponse.toResult
    )

  def deauthoriseRelationshipV3(givenArn: Arn, clientId: ApiClientId, service: ApiService): Action[AnyContent] =
    versionedAgentAction(givenArn)(
      v1V2 = _ =>
        Future.successful(
          if appConfig.v3Enabled then NotAcceptable("invalid client version, this endpoint requires one of [V3]")
          else NotFound("")
        ),
      v3 = request =>
        deauthoriseRelationshipV3Service
          .removeAuthorisation(givenArn, service, clientId)(using request)
          .map:
            case Right(_) | Left(NoRelationship) => NoContent
            case Left(errorResponse)             => errorResponse.toResult
    )
