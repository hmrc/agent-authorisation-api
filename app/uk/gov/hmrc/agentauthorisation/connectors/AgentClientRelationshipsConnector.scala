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

package uk.gov.hmrc.agentauthorisation.connectors

import play.api.http.Status.{CREATED, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models.{ApiErrorResponse, CreateInvitationRequestToAcr, InvitationDetails, StandardInternalServerError}
import uk.gov.hmrc.agentauthorisation.util.HttpAPIMonitor
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentClientRelationshipsConnector @Inject() (
  httpClient: HttpClientV2,
  val metrics: Metrics,
  appConfig: AppConfig
)(implicit val ec: ExecutionContext)
    extends HttpAPIMonitor {

  val acrUrl = url"${appConfig.acrBaseUrl}/agent-client-relationships"

  import uk.gov.hmrc.http.HttpReads.Implicits._

  def createInvitation(arn: Arn, validCreateInvitationRequest: CreateInvitationRequestToAcr)(implicit
    hc: HeaderCarrier
  ): Future[Either[ApiErrorResponse, InvitationId]] =
    monitor(s"ConsumedAPI-Agent-Create-Invitation-POST") {
      val requestUrl = url"$acrUrl/api/${arn.value}/invitation"
      httpClient
        .post(requestUrl)
        .withBody(Json.toJson(validCreateInvitationRequest))
        .execute[HttpResponse]
        .map {
          case response @ HttpResponse(CREATED, _, _) =>
            Right(InvitationId((response.json \ "invitationId").as[String]))
          case response =>
            Left(response.json.as[ApiErrorResponse])
        }
    }

  def getInvitation(arn: Arn, invitationId: InvitationId)(implicit
    hc: HeaderCarrier
  ): Future[Either[ApiErrorResponse, InvitationDetails]] =
    monitor(s"ConsumedAPI-Get-Invitation-GET") {
      val requestUrl = url"$acrUrl/api/${arn.value}/invitation/${invitationId.value}"
      httpClient
        .get(requestUrl)
        .execute[HttpResponse]
        .map {
          case response @ HttpResponse(OK, _, _) =>
            Right(response.json.as[InvitationDetails])
          case response =>
            Left(response.json.as[ApiErrorResponse])
        }
    }

  def cancelInvitation(invitationId: InvitationId)(implicit
    headerCarrier: HeaderCarrier
  ): Future[Either[ApiErrorResponse, Int]] =
    monitor(s"ConsumedAPI-Cancel-Invitation-PUT") {
      val requestUrl = url"$acrUrl/agent/cancel-invitation/${invitationId.value}"
      httpClient
        .put(requestUrl)
        .execute[HttpResponse]
        .map {
          case r if r.status == NO_CONTENT => Right(NO_CONTENT)
          case r =>
            Left(
              Json
                .fromJson[ApiErrorResponse](r.json)
                .getOrElse(StandardInternalServerError)
            )
        }
    }

}
