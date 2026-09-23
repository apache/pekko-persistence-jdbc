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

package org.apache.pekko.persistence.jdbc.snapshot.dao

import org.apache.pekko.persistence.{ SnapshotMetadata, SnapshotSelectionCriteria }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class SnapshotDaoSpec extends AnyWordSpec with Matchers with ScalaFutures {
  private class CustomSnapshotDao extends SnapshotDao {
    var calls = Vector.empty[(String, String, Seq[Long])]
    val snapshot = Some((SnapshotMetadata("custom", 3, 200), "snapshot"))

    private def loaded(method: String, persistenceId: String, bounds: Long*) = {
      calls :+= ((method, persistenceId, bounds.toList))
      Future.successful(snapshot)
    }

    private def deleted(method: String, persistenceId: String, bounds: Long*) = {
      calls :+= ((method, persistenceId, bounds.toList))
      Future.successful(())
    }

    override def latestSnapshot(persistenceId: String) = loaded("latest", persistenceId)
    override def snapshotForMaxTimestamp(persistenceId: String, timestamp: Long) =
      loaded("timestamp", persistenceId, timestamp)
    override def snapshotForMaxSequenceNr(persistenceId: String, sequenceNr: Long) =
      loaded("sequence", persistenceId, sequenceNr)
    override def snapshotForMaxSequenceNrAndMaxTimestamp(persistenceId: String, sequenceNr: Long, timestamp: Long) =
      loaded("both", persistenceId, sequenceNr, timestamp)

    override def deleteAllSnapshots(persistenceId: String) = deleted("latest", persistenceId)
    override def deleteUpToMaxTimestamp(persistenceId: String, timestamp: Long) =
      deleted("timestamp", persistenceId, timestamp)
    override def deleteUpToMaxSequenceNr(persistenceId: String, sequenceNr: Long) =
      deleted("sequence", persistenceId, sequenceNr)
    override def deleteUpToMaxSequenceNrAndMaxTimestamp(persistenceId: String, sequenceNr: Long, timestamp: Long) =
      deleted("both", persistenceId, sequenceNr, timestamp)

    override def delete(persistenceId: String, sequenceNr: Long) = deleted("single", persistenceId, sequenceNr)
    override def save(metadata: SnapshotMetadata, snapshot: Any) =
      Future.failed(new UnsupportedOperationException("save"))
  }

  "SnapshotDao criteria defaults" should {
    val upperBounds = Seq(
      (SnapshotSelectionCriteria.Latest, "latest", Seq.empty[Long]),
      (SnapshotSelectionCriteria(maxTimestamp = 200), "timestamp", Seq(200L)),
      (SnapshotSelectionCriteria(maxSequenceNr = 3), "sequence", Seq(3L)),
      (SnapshotSelectionCriteria(3, 200), "both", Seq(3L, 200L)))

    upperBounds.foreach { case (upper, method, arguments) =>
      s"preserve the existing $method load and delete methods" in {
        val dao = new CustomSnapshotDao
        Seq(upper, upper.copy(minSequenceNr = 1L, minTimestamp = 1L)).foreach { criteria =>
          dao.snapshotForCriteria("custom", criteria).futureValue shouldBe dao.snapshot
          dao.deleteByCriteria("custom", criteria).futureValue shouldBe (())
        }
        dao.calls shouldBe Vector.fill(4)((method, "custom", arguments))
      }
    }

    "filter an upper-bound result that does not satisfy a lower bound" in {
      val dao = new CustomSnapshotDao
      val criteria = SnapshotSelectionCriteria(minSequenceNr = 4L, minTimestamp = 201L)
      dao.snapshotForCriteria("custom", criteria).futureValue shouldBe None
      dao.calls shouldBe Vector(("latest", "custom", Seq.empty))

      dao.deleteByCriteria("custom", criteria).futureValue shouldBe (())
      dao.calls shouldBe Vector(
        ("latest", "custom", Seq.empty),
        ("latest", "custom", Seq.empty))
    }
  }
}
