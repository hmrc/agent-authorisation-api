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

package uk.gov.hmrc.agentauthorisation.wiring

import javax.inject.{Inject, Singleton}
import play.api.http.DefaultHttpFilters
import uk.gov.hmrc.agentauthorisation.actions.AcceptHeaderFilter
import uk.gov.hmrc.play.bootstrap.filters.{AuditFilter, CacheControlFilter, LoggingFilter}
import uk.gov.hmrc.play.bootstrap.metrics.MetricsFilter

@Singleton
class MicroserviceFilters @Inject() (
  metricsFilter: MetricsFilter,
  auditFilter: AuditFilter,
  loggingFilter: LoggingFilter,
  cacheFilter: CacheControlFilter,
  monitoringFilter: MicroserviceMonitoringFilter,
  acceptHeaderFilter: AcceptHeaderFilter
) extends DefaultHttpFilters(
      metricsFilter,
      monitoringFilter,
      auditFilter,
      loggingFilter,
      cacheFilter,
      acceptHeaderFilter
    )
