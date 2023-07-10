/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc

import scala.collection.immutable.NumericRange

/**
 * Efficient representation of missing elements using NumericRanges.
 * It can be seen as a collection of OrderingIds
 */
final case class MissingElements[A](elements: Seq[NumericRange[A]])(implicit num: Integral[A]) {
  def addRange(from: A, until: A): MissingElements[A] = {
    val newRange = NumericRange(from, until, num.one)
    MissingElements(elements :+ newRange)
  }
  def contains(id: A): Boolean = elements.exists(_.containsTyped(id))
  def isEmpty: Boolean = elements.forall(_.isEmpty)
  def size: Int = elements.map(_.size).sum
  override def toString: String = {
    elements
      .collect {
        case range if range.nonEmpty =>
          if (range.size == 1) range.start.toString
          else s"${range.start}-${range.end}"
      }
      .mkString(", ")
  }
}
object MissingElements {
  def empty[A: Integral]: MissingElements[A] = MissingElements(Vector.empty)
}
