/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId}

object UrlBinders {

  implicit val invitationIdBinder: PathBindable[InvitationId] =
    getInvitationIdBinder
  implicit object ArnBinder extends SimpleObjectBinder[Arn](Arn.apply, _.value)

  def getInvitationIdBinder(implicit stringBinder: PathBindable[String]) =
    new PathBindable[InvitationId] {

      override def bind(key: String, value: String): Either[String, InvitationId] = {
        val isValidPrefix =
          value.headOption.fold(false)(Seq('A', 'B', 'C').contains)

        if (isValidPrefix && InvitationId.isValid(value))
          Right(InvitationId(value))
        else
          Left(ErrorConstants.InvitationIdNotFound)
      }

      override def unbind(key: String, id: InvitationId): String =
        stringBinder.unbind(key, id.value)
    }
}

object ErrorConstants {
  val InvitationIdNotFound = "INVITATION_ID_NOTFOUND"
}
