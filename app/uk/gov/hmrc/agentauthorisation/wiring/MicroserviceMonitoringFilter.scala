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

import java.util.regex.{Matcher, Pattern}
import org.apache.pekko.stream.Materializer
import app.Routes
import com.codahale.metrics.MetricRegistry

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.http.HttpException

import scala.concurrent.duration.NANOSECONDS
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

@Singleton
class MicroserviceMonitoringFilter @Inject() (val metrics: Metrics, routes: Routes, val mat: Materializer)(implicit
  val ec: ExecutionContext
) extends MonitoringFilter(metrics.defaultRegistry) {
  override def keyToPatternMapping: Seq[(String, String)] = KeyToPatternMappingFromRoutes(routes, Set())
}

object KeyToPatternMappingFromRoutes extends Logging {
  def apply(routes: Routes, placeholders: Set[String]): Seq[(String, String)] =
    routes.documentation.map { case (method, route, _) =>
      val r = route.replace("<[^/]+>", "")
      val key = r
        .split("/")
        .map(p =>
          if (p.startsWith("$")) {
            val name = p.substring(1)
            if (placeholders.contains(name)) s"{$name}" else ":"
          } else p
        )
        .mkString("__")
      val pattern = r.replace("$", ":")
      logger.info(s"$key-$method -> $pattern")
      (key, pattern)
    }
}

abstract class MonitoringFilter(kenshooRegistry: MetricRegistry)(implicit ec: ExecutionContext)
    extends Filter with MonitoringKeyMatcher with Logging {

  override def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] =
    findMatchingKey(requestHeader.uri) match {
      case Some(key) =>
        monitor(s"API-$key-${requestHeader.method}") {
          nextFilter(requestHeader)
        }
      case None =>
        logger.debug(s"API-Not-Monitored: ${requestHeader.method}-${requestHeader.uri}")
        nextFilter(requestHeader)
    }

  private def monitor(serviceName: String)(function: => Future[Result])(implicit ec: ExecutionContext): Future[Result] =
    timer(serviceName) {
      function
    }

  private def timer(serviceName: String)(function: => Future[Result])(implicit ec: ExecutionContext): Future[Result] = {
    val start = System.nanoTime()
    function.andThen {
      case Success(result) =>
        val status = result.header.status
        val timerName = s"Timer-$serviceName"
        val counterName = timerName + "." + status
        kenshooRegistry.getTimers
          .getOrDefault(timerName, kenshooRegistry.timer(timerName))
          .update(System.nanoTime() - start, NANOSECONDS)
        kenshooRegistry.getCounters.getOrDefault(counterName, kenshooRegistry.counter(counterName)).inc()

      case Failure(exception: UpstreamErrorResponse) =>
        recordFailure(serviceName, exception.statusCode, start)
      case Failure(exception: HttpException) => recordFailure(serviceName, exception.responseCode, start)
      case Failure(_: Throwable)             => recordFailure(serviceName, 500, start)
    }
  }

  private def recordFailure(serviceName: String, upstreamResponseCode: Int, startTime: Long): Unit = {
    val timerName = s"Timer-$serviceName"
    val counterName =
      if (upstreamResponseCode >= 500) s"Http5xxErrorCount-$serviceName" else s"Http4xxErrorCount-$serviceName"
    kenshooRegistry.getTimers
      .getOrDefault(timerName, kenshooRegistry.timer(timerName))
      .update(System.nanoTime() - startTime, NANOSECONDS)
    kenshooRegistry.getCounters.getOrDefault(counterName, kenshooRegistry.counter(counterName)).inc()
  }
}

trait MonitoringKeyMatcher {

  private val placeholderPattern = Pattern.compile("(:[^/]+)")

  def keyToPatternMapping: Seq[(String, String)]

  private lazy val patterns: Seq[(String, (Pattern, Seq[String]))] = keyToPatternMapping
    .map { case (k, p) => (k, preparePatternAndVariables(p)) }
    .map { case (k, (p, vs)) => (k, (Pattern.compile(p), vs)) }

  def preparePatternAndVariables(p: String): (String, Seq[String]) = {
    var pattern = p
    val m = placeholderPattern.matcher(pattern)
    var variables = Seq[String]()
    while (m.find()) {
      val variable = m.group().substring(1)
      if (variables.contains(variable))
        throw new IllegalArgumentException(s"Duplicated variable name '$variable' in monitoring filter pattern '$p'")
      variables = variables :+ variable
    }
    for (v <- variables)
      pattern = pattern.replace(":" + v, "([^/]+)")
    ("^.*" + pattern + "$", variables.map("{" + _ + "}"))
  }

  def findMatchingKey(value: String): Option[String] =
    patterns.collectFirst {
      case (key, (pattern, variables)) if pattern.matcher(value).matches() =>
        (key, variables, readValues(pattern.matcher(value)))
    } map { case (key, variables, values) =>
      replaceVariables(key, variables, values)
    }

  private def readValues(result: Matcher): Seq[String] = {
    result.matches()
    (1 to result.groupCount()) map result.group
  }

  private def replaceVariables(key: String, variables: Seq[String], values: Seq[String]): String =
    if (values.isEmpty) key
    else
      values.zip(variables).foldLeft(key) { case (k, (value, variable)) =>
        k.replace(variable, value)
      }

}
