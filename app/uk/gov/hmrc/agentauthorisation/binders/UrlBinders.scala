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

package uk.gov.hmrc.agentauthorisation.binders

import play.api.mvc.PathBindable
import uk.gov.hmrc.agentauthorisation.models.{ApiClientId, ApiService, Arn, InvitationId}

object UrlBinders {

  given invitationIdBinder: PathBindable[InvitationId] =
    getInvitationIdBinder
  given apiServiceBinder: PathBindable[ApiService] = new PathBindable[ApiService] {
    override def bind(key: String, value: String): Either[String, ApiService] =
      ApiService.from(value).toRight(ErrorConstants.ServiceUnsupported)

    override def unbind(key: String, service: ApiService): String =
      service.value
  }
  given apiClientIdBinder: PathBindable[ApiClientId] = new PathBindable[ApiClientId] {
    override def bind(key: String, value: String): Either[String, ApiClientId] =
      ApiClientId.from(value).toRight(ErrorConstants.ClientIdInvalid)

    override def unbind(key: String, clientId: ApiClientId): String =
      clientId.value
  }
  given arnBinder: PathBindable[Arn] = new PathBindable[Arn] {
    override def bind(key: String, value: String): Either[String, Arn] =
      if (Arn.isValid(value)) Right(Arn(value)) else Left(ErrorConstants.ArnInvalid)

    override def unbind(key: String, arn: Arn): String =
      arn.value
  }

  def getInvitationIdBinder(using stringBinder: PathBindable[String]): PathBindable[InvitationId] =
    new PathBindable[InvitationId] {

      override def bind(key: String, value: String): Either[String, InvitationId] = {
        val isValidPrefix =
          value.headOption.fold(false)(Seq('A', 'B', 'C', 'L').contains)

        if (isValidPrefix && InvitationId.isValid(value))
          Right(InvitationId(value))
        else
          Left(ErrorConstants.InvitationIdInvalid)
      }

      override def unbind(key: String, id: InvitationId): String =
        stringBinder.unbind(key, id.value)
    }
}

object ErrorConstants {
  val ArnInvalid = "ARN_FORMAT_INVALID"
  val ClientIdInvalid = "CLIENT_ID_FORMAT_INVALID"
  val InvitationIdInvalid = "INVITATION_ID_FORMAT_INVALID"
  val ServiceUnsupported = "SERVICE_NOT_SUPPORTED"
}
