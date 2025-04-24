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

import play.api.http.Status.{FORBIDDEN, LOCKED, NOT_FOUND, NO_CONTENT}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.agentauthorisation.config.AppConfig
import uk.gov.hmrc.agentauthorisation.connectors.Syntax.intOps
import uk.gov.hmrc.agentauthorisation.models._
import uk.gov.hmrc.agentauthorisation.util.HttpAPIMonitor
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InvitationsConnector @Inject() (httpClient: HttpClientV2, val metrics: Metrics, appConfig: AppConfig)(implicit
  val ec: ExecutionContext
) extends HttpAPIMonitor {

  val acaUrl = s"${appConfig.acaBaseUrl}/agent-client-authorisation"

  import uk.gov.hmrc.http.HttpReads.Implicits._

  private val isoDateFormat = DateTimeFormatter.ISO_LOCAL_DATE

  def createInvitation(arn: Arn, agentInvitation: AgentInvitation)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[String]] =
    monitor(s"ConsumedAPI-Agent-Create-Invitation-POST") {
      val requestUrl = url"$acaUrl/agencies/${arn.value}/invitations/sent"
      httpClient
        .post(requestUrl)
        .setHeader("Origin" -> "agent-authorisation-api")
        .withBody(Json.toJson(agentInvitation))
        .execute[HttpResponse]
        .map {
          case r if r.status.isSuccess => r.header("InvitationId")
          case r =>
            throw UpstreamErrorResponse
              .apply(s"POST of '$requestUrl' returned ${r.status}. Response body: '${r.body}'", r.status)
        }
    }

  def checkPostcodeForClient(nino: Nino, postcode: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[KnownFactCheckResult] =
    monitor(s"ConsumedAPI-CheckPostcode-GET") {
      val requestUrl = url"$acaUrl/known-facts/individuals/nino/${nino.value}/sa/postcode/$postcode"
      httpClient
        .get(requestUrl)
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case NO_CONTENT => KnownFactCheckPassed
            case FORBIDDEN =>
              KnownFactCheckFailed((response.json \ "code").as[String])
            case other => KnownFactCheckFailed(s"Failed due to status $other")
          }
        }
    }

  def checkVatRegDateForClient(vrn: Vrn, registrationDateKnownFact: LocalDate)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[KnownFactCheckResult] =
    monitor(s"ConsumedAPI-CheckVatRegDate-GET") {
      val requestUrl =
        url"$acaUrl/known-facts/organisations/vat/${vrn.value}/registration-date/${registrationDateKnownFact.toString}"
      httpClient
        .get(requestUrl)
        .execute[HttpResponse]
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

  def getInvitation(arn: Arn, invitationId: InvitationId)(implicit
    headerCarrier: HeaderCarrier
  ): Future[Option[StoredInvitation]] =
    monitor(s"ConsumedAPI-Get-Invitation-GET") {
      val requestUrl = url"$acaUrl/agencies/${arn.value}/invitations/sent/${invitationId.value}"
      httpClient
        .get(requestUrl)
        .execute[Option[StoredInvitation]]
    }.recoverWith { case _ =>
      Future successful None
    }

  def cancelInvitation(arn: Arn, invitationId: InvitationId)(implicit
    headerCarrier: HeaderCarrier
  ): Future[Option[Int]] =
    monitor(s"ConsumedAPI-Cancel-Invitation-PUT") {
      val requestUrl = url"$acaUrl/agencies/${arn.value}/invitations/sent/${invitationId.value}/cancel"
      httpClient
        .put(requestUrl)
        .withBody("")
        .execute[HttpResponse]
        .map {
          case r if r.body.contains("INVALID_INVITATION_STATUS") => Some(500)
          case response                                          => Some(response.status)
        }
    }

  def getAllInvitations(arn: Arn, createdOnOrAfter: LocalDate)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Seq[StoredInvitation]] =
    monitor(s"ConsumedAPI-Get-AllInvitations-GET") {
      val requestUrl =
        url"$acaUrl/agencies/${arn.value}/invitations/sent?service=HMRC-MTD-IT,HMRC-MTD-IT-SUPP,HMRC-MTD-VAT&createdOnOrAfter=${createdOnOrAfter
          .format(isoDateFormat)}"
      httpClient
        .get(requestUrl)
        .execute[JsObject]
        .map(obj => (obj \ "_embedded" \ "invitations").as[Seq[StoredInvitation]])
    }

  def getAllInvitationsForClient(arn: Arn, clientId: String, serviceName: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Seq[StoredInvitation]] =
    monitor("ConsumedAPI-PendingInvitationsExistForClient-GET") {
      val requestUrl = url"$acaUrl/agencies/${arn.value}/invitations/sent?clientId=$clientId&service=$serviceName"
      httpClient
        .get(requestUrl)
        .execute[JsObject]
        .map(obj => (obj \ "_embedded" \ "invitations").as[Seq[StoredInvitation]])
    }
}
