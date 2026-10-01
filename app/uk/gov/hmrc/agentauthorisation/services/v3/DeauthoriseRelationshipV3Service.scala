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

package uk.gov.hmrc.agentauthorisation.services.v3

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.models.v3.ApiServiceConfiguration
import uk.gov.hmrc.agentauthorisation.models.{ApiClientId, ApiErrorResponse, ApiService, Arn, ClientIdNotCompatibleWithService, NoRelationship}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeauthoriseRelationshipV3Service @Inject() (
  acrConnector: AgentClientRelationshipsConnector
)(using ExecutionContext):
  def removeAuthorisation(arn: Arn, service: ApiService, clientId: ApiClientId)(using
    RequestHeader
  ): Future[Either[ApiErrorResponse, Unit]] =
    if !service.accepts(clientId) then Future.successful(Left(ClientIdNotCompatibleWithService))
    else
      val conf = ApiServiceConfiguration.forCommand(service, clientId)

      val main = acrConnector.removeAuthorisation(arn, clientId.value, conf.detailsService)

      val associated = conf.associatedService match
        case Some(associatedService) => acrConnector.removeAuthorisation(arn, clientId.value, associatedService)
        case None                    => Future.successful(Right(()))

      main.flatMap:
        case Right(()) | Left(NoRelationship) =>
          associated.map:
            case Right(()) | Left(NoRelationship) => Right(())
            case Left(error)                      => Left(error)

        case other => Future.successful(other)
