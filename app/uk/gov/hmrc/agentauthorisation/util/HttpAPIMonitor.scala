/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentauthorisation.util

import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.concurrent.{ExecutionContext, Future}

trait HttpAPIMonitor {

  val metrics: Metrics
  implicit val ec: ExecutionContext
  def monitor[A](str: String)(f: => Future[A]): Future[A] = {
    val timerContext = metrics.defaultRegistry.timer(s"Timer-$str").time()
    f.andThen { case _ => timerContext.stop() }
  }

  def reportHistogramValue[T](name: String, value: Long): Unit =
    metrics.defaultRegistry
      .getHistograms()
      .getOrDefault(histogramName(name), metrics.defaultRegistry.histogram(histogramName(name)))
      .update(value)

  def histogramName[T](counterName: String): String =
    s"Histogram-$counterName"
}
