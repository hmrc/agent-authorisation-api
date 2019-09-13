/*
 * Copyright 2019 HM Revenue & Customs
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

import java.net.URL

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Named, Singleton}
import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.JsObject
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentauthorisation.models.{AgentInvitation, StoredInvitation}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, Vrn}
import uk.gov.hmrc.agentauthorisation.UriPathEncoding.encodePathSegment
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InvitationsConnector @Inject()(
  @Named("agent-client-authorisation-baseUrl") baseUrl: URL,
  http: HttpPost with HttpGet with HttpPut,
  metrics: Metrics)
    extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private val dateFormatter = ISODateTimeFormat.date()

  private[connectors] def createAgentLinkUrl(arn: Arn, clientType: String): URL =
    new URL(
      baseUrl,
      s"/agent-client-authorisation/agencies/references/arn/${encodePathSegment(arn.value)}/clientType/$clientType")

  private[connectors] def createInvitationUrl(arn: Arn): URL =
    new URL(baseUrl, s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent")

  private[connectors] def checkPostcodeUrl(nino: Nino, postcode: String) =
    new URL(baseUrl, s"/agent-client-authorisation/known-facts/individuals/nino/${nino.value}/sa/postcode/$postcode")

  private[connectors] def checkVatRegisteredClientUrl(vrn: Vrn, registrationDate: LocalDate) =
    new URL(
      baseUrl,
      s"/agent-client-authorisation/known-facts/organisations/vat/${vrn.value}/registration-date/${registrationDate.toString}")

  private[connectors] def getInvitationUrl(arn: Arn, invitationId: InvitationId) =
    new URL(baseUrl, s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}")

  private[connectors] def cancelInvitationUrl(arn: Arn, invitationId: InvitationId) =
    new URL(baseUrl, s"/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}/cancel")

  private[connectors] def getAgencyInvitationsUrl(arn: Arn, createdOnOrAfter: LocalDate): URL =
    new URL(
      baseUrl,
      s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent?service=HMRC-MTD-IT,HMRC-MTD-VAT&createdOnOrAfter=${dateFormatter
        .print(createdOnOrAfter)}"
    )

  def createInvitation(arn: Arn, agentInvitation: AgentInvitation)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[String]] =
    monitor(s"ConsumedAPI-Agent-Create-Invitation-POST") {
      http.POST[AgentInvitation, HttpResponse](createInvitationUrl(arn).toString, agentInvitation) map { r =>
        r.header("InvitationId")
      }
    }

  def checkPostcodeForClient(nino: Nino, postcode: String)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[Boolean]] =
    monitor(s"ConsumedAPI-CheckPostcode-GET") {
      http
        .GET[HttpResponse](checkPostcodeUrl(nino, postcode).toString)
        .map(_ => Some(true))
    }.recover {
      case notMatched: Upstream4xxResponse if notMatched.message.contains("POSTCODE_DOES_NOT_MATCH") =>
        Some(false)
      case notEnrolled: Upstream4xxResponse if notEnrolled.message.contains("CLIENT_REGISTRATION_NOT_FOUND") =>
        None
    }

  def checkVatRegDateForClient(vrn: Vrn, registrationDateKnownFact: LocalDate)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[Boolean]] =
    monitor(s"ConsumedAPI-CheckVatRegDate-GET") {
      http
        .GET[HttpResponse](checkVatRegisteredClientUrl(vrn, registrationDateKnownFact).toString)
        .map(_ => Some(true))
    }.recover {
      case ex: Upstream4xxResponse if ex.upstreamResponseCode == 403 =>
        Some(false)
      case _: NotFoundException => None
    }

  def getInvitation(arn: Arn, invitationId: InvitationId)(
    implicit
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext) =
    monitor(s"ConsumedAPI-Get-Invitation-GET") {
      http.GET[Option[StoredInvitation]](getInvitationUrl(arn, invitationId).toString)
    }.recoverWith {
      case _ => Future successful None
    }

  def cancelInvitation(arn: Arn, invitationId: InvitationId)(
    implicit
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext): Future[Option[Int]] =
    monitor(s"ConsumedAPI-Cancel-Invitation-PUT") {
      http
        .PUT[String, HttpResponse](cancelInvitationUrl(arn, invitationId).toString, "")
        .map(response => Some(response.status))
    }.recover {
      case _: NotFoundException => Some(404)
      case ex: Upstream4xxResponse if ex.message.contains("INVALID_INVITATION_STATUS") =>
        Some(500)
      case _ => Some(403)
    }

  def getAllInvitations(arn: Arn, createdOnOrAfter: LocalDate)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Seq[StoredInvitation]] =
    monitor(s"ConsumedAPI-Get-AllInvitations-GET") {
      val url = getAgencyInvitationsUrl(arn, createdOnOrAfter)
      http
        .GET[JsObject](url.toString)
        .map(obj => {
          (obj \ "_embedded" \ "invitations").as[Seq[StoredInvitation]]
        })
    }
}
