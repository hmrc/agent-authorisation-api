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

package uk.gov.hmrc.agentauthorisation.services

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class FakeLockService extends MongoLockService {
  val locked: mutable.Set[(String, String, String)] = mutable.Set.empty[(String, String, String)]

  override def acquireLock[T](arn: String, service: String, clientId: String)(
    body: => Future[T]
  )(implicit ec: ExecutionContext): Future[Option[T]] =
    if (locked.contains((arn, service, clientId))) Future.successful(None)
    else {
      locked.add((arn, service, clientId))
      body.map(Some.apply).map { result =>
        locked.remove((arn, service, clientId))
        result
      }
    }
}
