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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.api.http.Status.{FORBIDDEN, LOCKED, NOT_FOUND, NO_CONTENT}
import play.api.libs.json.JsObject
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentauthorisation.UriPathEncoding.encodePathSegment
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.connectors.Syntax._
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InvitationsConnector @Inject()(httpClient: HttpClient, metrics: Metrics, appConfig: AppConfig)
    extends HttpAPIMonitor {

  val acaUrl = s"${appConfig.acaBaseUrl}/agent-client-authorisation"

  import uk.gov.hmrc.http.HttpReads.Implicits._

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private val isoDateFormat = DateTimeFormatter.ISO_LOCAL_DATE

  private[connectors] def createAgentLinkUrl(arn: Arn, clientType: String): URL =
    new URL(s"$acaUrl/agencies/references/arn/${encodePathSegment(arn.value)}/clientType/$clientType")

  private[connectors] def createInvitationUrl(arn: Arn): URL =
    new URL(s"$acaUrl/agencies/${encodePathSegment(arn.value)}/invitations/sent")

  private[connectors] def checkPostcodeUrl(nino: Nino, postcode: String) =
    new URL(s"$acaUrl/known-facts/individuals/nino/${nino.value}/sa/postcode/$postcode")

  private[connectors] def checkVatRegisteredClientUrl(vrn: Vrn, registrationDate: LocalDate) =
    new URL(s"$acaUrl/known-facts/organisations/vat/${vrn.value}/registration-date/${registrationDate.toString}")

  private[connectors] def getInvitationUrl(arn: Arn, invitationId: InvitationId) =
    new URL(s"$acaUrl/agencies/${arn.value}/invitations/sent/${invitationId.value}")

  private[connectors] def cancelInvitationUrl(arn: Arn, invitationId: InvitationId) =
    new URL(s"$acaUrl/agencies/${arn.value}/invitations/sent/${invitationId.value}/cancel")

  private[connectors] def getAgencyInvitationsUrl(arn: Arn, createdOnOrAfter: LocalDate): URL =
    new URL(
      s"$acaUrl/agencies/${encodePathSegment(arn.value)}/invitations/sent?service=HMRC-MTD-IT,HMRC-MTD-VAT&createdOnOrAfter=${createdOnOrAfter
        .format(isoDateFormat)}"
    )

  private[connectors] def getAllInvitationsForClientUrl(arn: Arn, clientId: String, service: Service): URL =
    new URL(
      s"$acaUrl/agencies/${encodePathSegment(arn.value)}/invitations/sent?clientId=$clientId&service=${service.toString}"
    )

  private[connectors] def getAllPendingInvitationsForClientUrl(arn: Arn, clientId: String, service: Service): URL =
    new URL(
      s"$acaUrl/agencies/${encodePathSegment(arn.value)}/invitations/sent?status=Pending&clientId=$clientId&service=${service.toString}"
    )

  def createInvitation(arn: Arn, agentInvitation: AgentInvitation)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[String]] =
    monitor(s"ConsumedAPI-Agent-Create-Invitation-POST") {
      val url = createInvitationUrl(arn).toString
      httpClient
        .POST[AgentInvitation, HttpResponse](
          url,
          agentInvitation,
          Seq("Origin" -> "agent-authorisation-api")
        )
        .map {
          case r if r.status.isSuccess => r.header("InvitationId")
          case r =>
            throw UpstreamErrorResponse
              .apply(s"POST of '$url' returned ${r.status}. Response body: '${r.body}'", r.status)
        }
    }

  def checkPostcodeForClient(nino: Nino, postcode: String)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[KnownFactCheckResult] =
    monitor(s"ConsumedAPI-CheckPostcode-GET") {
      httpClient
        .GET[HttpResponse](checkPostcodeUrl(nino, postcode).toString)
        .map { response =>
          response.status match {
            case NO_CONTENT => KnownFactCheckPassed
            case FORBIDDEN =>
              KnownFactCheckFailed((response.json \ "code").as[String])
            case other => KnownFactCheckFailed(s"Failed due to status $other")
          }
        }
    }

  def checkVatRegDateForClient(vrn: Vrn, registrationDateKnownFact: LocalDate)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[KnownFactCheckResult] =
    monitor(s"ConsumedAPI-CheckVatRegDate-GET") {
      httpClient
        .GET[HttpResponse](checkVatRegisteredClientUrl(vrn, registrationDateKnownFact).toString)
        .map { response =>
          response.status match {
            case NO_CONTENT => KnownFactCheckPassed
            case FORBIDDEN  => KnownFactCheckFailed((response.json \ "code").as[String])
            case NOT_FOUND  => KnownFactCheckFailed("VAT_RECORD_NOT_FOUND")
            case LOCKED     => KnownFactCheckFailed("MIGRATION_IN_PROGRESS")
            case other      => KnownFactCheckFailed(s"Failed due to status $other")
          }
        }
    }

  def getInvitation(arn: Arn, invitationId: InvitationId)(
    implicit
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext): Future[Option[StoredInvitation]] =
    monitor(s"ConsumedAPI-Get-Invitation-GET") {
      httpClient
        .GET[Option[StoredInvitation]](getInvitationUrl(arn, invitationId).toString)
    }.recoverWith {
      case _ => Future successful None
    }

  def cancelInvitation(arn: Arn, invitationId: InvitationId)(
    implicit
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext): Future[Option[Int]] =
    monitor(s"ConsumedAPI-Cancel-Invitation-PUT") {
      httpClient
        .PUT[String, HttpResponse](cancelInvitationUrl(arn, invitationId).toString, "")
        .map {
          case r if r.body.contains("INVALID_INVITATION_STATUS") => Some(500)
          case response                                          => Some(response.status)
        }
    }

  def getAllInvitations(arn: Arn, createdOnOrAfter: LocalDate)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Seq[StoredInvitation]] =
    monitor(s"ConsumedAPI-Get-AllInvitations-GET") {
      val url = getAgencyInvitationsUrl(arn, createdOnOrAfter)
      httpClient
        .GET[JsObject](url.toString)
        .map(obj => {
          (obj \ "_embedded" \ "invitations").as[Seq[StoredInvitation]]
        })
    }

  def getAllInvitationsForClient(arn: Arn, clientId: String, service: Service)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Seq[StoredInvitation]] =
    monitor("ConsumedAPI-PendingInvitationsExistForClient-GET") {
      val url = getAllInvitationsForClientUrl(arn, clientId, service)
      httpClient
        .GET[JsObject](url.toString)
        .map(obj => {
          (obj \ "_embedded" \ "invitations").as[Seq[StoredInvitation]]
        })
    }

  def pendingInvitationsExistForClient(arn: Arn, clientId: String, service: Service)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Boolean] =
    monitor("ConsumedAPI-PendingInvitationsExistForClient-GET") {
      val url = getAllPendingInvitationsForClientUrl(arn, clientId, service)
      httpClient
        .GET[JsObject](url.toString)
        .map(obj => {
          (obj \ "_embedded" \ "invitations").as[Seq[StoredInvitation]]
        }.nonEmpty)
    }
}
