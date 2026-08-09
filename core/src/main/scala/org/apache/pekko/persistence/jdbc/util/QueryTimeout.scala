/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.persistence.jdbc.util

import java.util.concurrent.{ Executors, ThreadFactory, TimeUnit, TimeoutException }
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.concurrent.duration.FiniteDuration

/**
 * Provides timeout wrapping for database operations. Uses a daemon thread scheduler to avoid blocking actor threads.
 */
object QueryTimeout {

  private val scheduler = Executors.newScheduledThreadPool(1,
    new ThreadFactory {
      override def newThread(r: Runnable): Thread = {
        val t = new Thread(r, "pekko-persistence-jdbc-query-timeout")
        t.setDaemon(true)
        t
      }
    })

  /**
   * Wraps a Future with a timeout. If the timeout duration is zero or negative, the original Future is returned
   * unchanged (timeout disabled).
   */
  def withTimeout[T](future: => Future[T], timeout: FiniteDuration)(implicit ec: ExecutionContext): Future[T] = {
    if (timeout.length <= 0) future
    else {
      val promise = Promise[T]()
      val scheduled = scheduler.schedule(
        new Runnable {
          override def run(): Unit =
            promise.tryFailure(new TimeoutException(s"Database operation timed out after $timeout"))
        },
        timeout.toMillis,
        TimeUnit.MILLISECONDS)
      future.onComplete(_ => scheduled.cancel(false))(ExecutionContext.parasitic)
      Future.firstCompletedOf(Seq(future, promise.future))
    }
  }
}
