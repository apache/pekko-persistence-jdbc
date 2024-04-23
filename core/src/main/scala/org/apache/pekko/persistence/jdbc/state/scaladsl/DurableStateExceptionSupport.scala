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

package org.apache.pekko.persistence.jdbc.state.scaladsl

import java.lang.invoke.{ MethodHandles, MethodType }

import scala.util.Try

/**
  * INTERNAL API
  *
  * Support for creating a `DeleteRevisionException`if the class is
  * available on the classpath. Pekko 1.0 does not have this class, but
  * it is added in Pekko 1.1.
  */
private[scaladsl] object DurableStateExceptionSupport {
  val DeleteRevisionExceptionClass =
    "org.apache.pekko.persistence.state.exception.DeleteRevisionException"

  private def exceptionClassOpt: Option[Class[_]] =
    Try(Class.forName(DeleteRevisionExceptionClass)).toOption

  private val constructorOpt = exceptionClassOpt.map { clz =>
    val mt = MethodType.methodType(classOf[Unit], classOf[String])
    MethodHandles.publicLookup().findConstructor(clz, mt)
  }

  def createDeleteRevisionExceptionIfSupported(message: String): Option[Exception] =
    constructorOpt.map { constructor =>
      constructor.invoke(message).asInstanceOf[Exception]
    }

}
