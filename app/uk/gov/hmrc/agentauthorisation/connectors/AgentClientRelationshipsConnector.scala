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

import play.api.http.Status.{CREATED, NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.models.{Arn, InvitationId}
import uk.gov.hmrc.agentauthorisation.models.v3.{
  AcrAgentLinkV3,
  AcrClientDetails,
  AcrCreateInvitationRequest,
  AcrInvitationPageV3,
  AcrInvitationV3,
  CreateInvitationV3Response
}
import uk.gov.hmrc.agentauthorisation.util.RequestSupport.given
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class AgentClientRelationshipsConnector @Inject() (
  httpClient: HttpClientV2,
  val metrics: Metrics,
  appConfig: AppConfig
)(using val ec: ExecutionContext) {

  private val acrUrl = url"${appConfig.acrBaseUrl}/agent-client-relationships"
  private val invitationPageSize = 100

  def getClientDetails(service: String, clientId: String)(using
    rh: RequestHeader
  ): Future[Either[ApiErrorResponse, AcrClientDetails]] =
    httpClient
      .get(url"$acrUrl/client/$service/details/$clientId")
      .execute[HttpResponse]
      .map {
        case response @ HttpResponse(OK, _, _) => Right(response.json.as[AcrClientDetails])
        case HttpResponse(NOT_FOUND, _, _)      => Left(ClientRegistrationNotFound)
        case _                                  => Left(StandardInternalServerError)
      }

  def createInvitationV3(arn: Arn, request: AcrCreateInvitationRequest)(using
    rh: RequestHeader
  ): Future[Either[ApiErrorResponse, InvitationId]] =
    httpClient
      .post(url"$acrUrl/agent/${arn.value}/authorisation-request")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map {
        case response @ HttpResponse(CREATED, _, _) =>
          Right(InvitationId(response.json.as[CreateInvitationV3Response].invitationId))
        case response if errorCode(response).contains("DUPLICATE_AUTHORISATION_REQUEST") =>
          Left(DuplicateAuthorisationRequestV3)
        case response if errorCode(response).contains("ALREADY_AUTHORISED") =>
          Left(AlreadyAuthorised)
        case _ => Left(StandardInternalServerError)
      }

  private def errorCode(response: HttpResponse): Option[String] =
    Try((response.json \ "code").asOpt[String]).toOption.flatten

  def getInvitationsV3(arn: Arn)(using
    rh: RequestHeader
  ): Future[Either[ApiErrorResponse, Seq[AcrInvitationV3]]] =
    getInvitationsPageV3(arn, pageNumber = 1).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(firstPage) =>
        val remainingPages = 2 to Math.ceil(firstPage.totalResults.toDouble / invitationPageSize).toInt
        Future
          .traverse(remainingPages)(getInvitationsPageV3(arn, _))
          .map { pages =>
            pages.foldLeft[Either[ApiErrorResponse, Vector[AcrInvitationV3]]](Right(firstPage.requests.toVector)) {
              case (Left(error), _)                  => Left(error)
              case (_, Left(error))                  => Left(error)
              case (Right(invitations), Right(page)) => Right(invitations ++ page.requests)
            }
          }
    }

  private def getInvitationsPageV3(arn: Arn, pageNumber: Int)(using
    rh: RequestHeader
  ): Future[Either[ApiErrorResponse, AcrInvitationPageV3]] =
    httpClient
      .get(
        url"$acrUrl/agent/${arn.value}/authorisation-requests?pageNumber=$pageNumber&pageSize=$invitationPageSize"
      )
      .execute[HttpResponse]
      .map {
        case response @ HttpResponse(OK, _, _) => Right(response.json.as[AcrInvitationPageV3])
        case _                                  => Left(StandardInternalServerError)
      }

  def getAgentLinkV3()(using
    rh: RequestHeader
  ): Future[Either[ApiErrorResponse, AcrAgentLinkV3]] =
    httpClient
      .get(url"$acrUrl/agent/agent-link")
      .execute[HttpResponse]
      .map {
        case response @ HttpResponse(OK, _, _) => Right(response.json.as[AcrAgentLinkV3])
        case _                                  => Left(StandardInternalServerError)
      }

  def createInvitation(arn: Arn, clientAccessData: ClientAccessData)(using
    rh: RequestHeader
  ): Future[Either[ApiErrorResponse, InvitationId]] = {
    val requestUrl = url"$acrUrl/api/${arn.value}/invitation"
    httpClient
      .post(requestUrl)
      .withBody(Json.toJson(clientAccessData))
      .execute[HttpResponse]
      .map {
        case response @ HttpResponse(CREATED, _, _) =>
          Right(InvitationId((response.json \ "invitationId").as[String]))
        case response =>
          Left(response.json.as[ApiErrorResponse](using ApiErrorResponse.acrReads(Some(clientAccessData.service))))
      }
  }

  def getInvitation(arn: Arn, invitationId: InvitationId)(using
    rh: RequestHeader
  ): Future[Either[ApiErrorResponse, SingleInvitationDetails]] = {
    val requestUrl = url"$acrUrl/api/${arn.value}/invitation/${invitationId.value}"
    httpClient
      .get(requestUrl)
      .execute[HttpResponse]
      .map {
        case response @ HttpResponse(OK, _, _) =>
          Right(response.json.as[SingleInvitationDetails])
        case response =>
          Left(response.json.as[ApiErrorResponse](using ApiErrorResponse.acrReads()))
      }
  }

  def getAllInvitations(arn: Arn)(using
    rh: RequestHeader
  ): Future[Either[ApiErrorResponse, AllInvitationDetails]] = {
    val requestUrl = url"$acrUrl/api/${arn.value}/invitations"
    httpClient
      .get(requestUrl)
      .execute[HttpResponse]
      .map {
        case response @ HttpResponse(OK, _, _) =>
          Right(response.json.as[AllInvitationDetails])
        case response =>
          Left(response.json.as[ApiErrorResponse](using ApiErrorResponse.acrReads()))
      }
  }

  def cancelInvitation(invitationId: InvitationId)(using
    rh: RequestHeader
  ): Future[Either[ApiErrorResponse, Int]] = {
    val requestUrl = url"$acrUrl/agent/cancel-invitation/${invitationId.value}"
    httpClient
      .put(requestUrl)
      .execute[HttpResponse]
      .map {
        case HttpResponse(NO_CONTENT, _, _) =>
          Right(NO_CONTENT)
        case response =>
          Left(response.json.as[ApiErrorResponse](using ApiErrorResponse.acrReads()))
      }
  }

  def checkRelationship(arn: Arn, clientAccessData: ClientAccessData)(using
    rh: RequestHeader
  ): Future[Either[ApiErrorResponse, Boolean]] =
    httpClient
      .post(url"$acrUrl/api/${arn.value}/relationship")
      .withBody(Json.toJson(clientAccessData))
      .execute[HttpResponse]
      .map {
        case HttpResponse(NO_CONTENT, _, _) =>
          Right(true)
        case response =>
          Left(response.json.as[ApiErrorResponse](using ApiErrorResponse.acrReads(Some(clientAccessData.service))))
      }

  def removeAuthorisation(arn: Arn, clientId: String, service: String)(using
    rh: RequestHeader
  ): Future[Either[ApiErrorResponse, Unit]] = {
    val requestUrl = url"$acrUrl/agent/${arn.value}/remove-authorisation"
    val body = Json.obj(
      "clientId" -> clientId,
      "service"  -> service
    )

    httpClient
      .post(requestUrl)
      .withBody(body)
      .execute[HttpResponse]
      .map {
        case HttpResponse(NO_CONTENT, _, _) =>
          Right(())
        case response =>
          val json = response.json
          (json \ "code").asOpt[String] match {
            case Some("RELATIONSHIP_NOT_FOUND") =>
              Left(NoRelationship)
            case _ =>
              Left(json.as[ApiErrorResponse](using ApiErrorResponse.acrReads()))
          }
      }
  }
}
