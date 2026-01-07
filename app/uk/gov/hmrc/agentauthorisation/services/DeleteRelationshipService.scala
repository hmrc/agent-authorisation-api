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

package uk.gov.hmrc.agentauthorisation.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentauthorisation.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.agentauthorisation.models.Service.{ItsaMain, ItsaSupp, Vat}
import uk.gov.hmrc.agentauthorisation.models.{ApiErrorResponse, Arn, DeleteRelationshipPayload, LockedRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteRelationshipService @Inject() (
  lockService: MongoLockService,
  acrConnector: AgentClientRelationshipsConnector
)(implicit ec: ExecutionContext) {

  def deleteRelationship(
    arn: Arn,
    payload: DeleteRelationshipPayload
  )(implicit rh: RequestHeader): Future[Either[ApiErrorResponse, Unit]] = {
    val externalService = payload.service.head
    val clientId = payload.clientId

    val acrService = mapExternalToAcrService(externalService, payload.agentType)

    lockService
      .acquireLock(
        arn = arn.value,
        service = acrService,
        clientId = clientId
      ) {
        acrConnector.removeAuthorisation(arn, clientId, acrService)
      }
      .map {
        case Some(res) => res
        case None      => Left(LockedRequest)
      }
  }

  private val itsaExternalService: String = ItsaMain.externalServiceName
  private val vatExternalService: String = Vat.externalServiceName

  /** Map external service codes used by this API to the internal service ids expected by agent-client-relationships.
    *
    * Existing endpoints (create invitation / check relationship) follow the pattern: external code → `Service` ADT →
    * internal service name. For the delete endpoint we keep the payload as strings but still translate to the internal
    * identifiers here before calling ACR and acquiring locks.
    */
  private def mapExternalToAcrService(external: String, agentType: Option[String]): String =
    external match {
      case `itsaExternalService` =>
        agentType match {
          case Some("supporting") => ItsaSupp.internalServiceName // "HMRC-MTD-IT-SUPP"
          case _                  => ItsaMain.internalServiceName // "HMRC-MTD-IT"
        }
      case `vatExternalService` =>
        Vat.internalServiceName // "HMRC-MTD-VAT"
      case other =>
        // validateDeleteRelationshipPayload only allows MTD-IT / MTD-VAT,
        // so this branch is effectively defensive.
        other
    }
}
