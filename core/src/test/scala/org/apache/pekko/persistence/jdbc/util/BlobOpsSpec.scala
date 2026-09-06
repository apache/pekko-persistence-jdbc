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

import java.io.{ ByteArrayInputStream, InputStream }

import javax.sql.rowset.serial.SerialBlob

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

object BlobOpsSpec {

  /**
   * A blob that keeps track of the binary streams it handed out, like the ones a JDBC driver creates for a lob that
   * is read from the database.
   */
  class TrackingBlob(bytes: Array[Byte]) extends SerialBlob(bytes) {
    @volatile var openStreams: Int = 0

    override def getBinaryStream: InputStream = {
      openStreams += 1
      new ByteArrayInputStream(bytes) {
        override def close(): Unit = {
          openStreams -= 1
          super.close()
        }
      }
    }
  }
}

class BlobOpsSpec extends AnyWordSpecLike with Matchers {
  import BlobOpsSpec._

  "BlobOps.toArray" must {
    "read the content of the blob and close the stream it read from" in {
      val bytes = Array[Byte](1, 2, 3, 4, 5)
      val blob = new TrackingBlob(bytes)

      BlobOps.toArray(blob) shouldBe bytes

      blob.openStreams shouldBe 0
    }

    "close the stream when reading fails" in {
      val blob = new TrackingBlob(Array[Byte](1, 2, 3)) {
        override def getBinaryStream: InputStream = {
          val delegate = super.getBinaryStream
          new InputStream {
            override def read(): Int = throw new RuntimeException("boom")
            override def read(b: Array[Byte], off: Int, len: Int): Int = throw new RuntimeException("boom")
            override def close(): Unit = delegate.close()
          }
        }
      }

      a[RuntimeException] should be thrownBy BlobOps.toArray(blob)

      blob.openStreams shouldBe 0
    }
  }
}
