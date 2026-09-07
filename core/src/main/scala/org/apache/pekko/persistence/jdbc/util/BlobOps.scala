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

import java.sql.Blob

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.persistence.jdbc.util.InputStreamOps.InputStreamImplicits

/**
 * INTERNAL API
 */
@InternalApi
private[jdbc] object BlobOps {

  /**
   * Reads the complete content of the blob and closes the stream that was used to read it. A stream that is left
   * open keeps driver resources (a cursor or a temporary lob) allocated for every row that is read.
   */
  def toArray(blob: Blob): Array[Byte] = {
    val inputStream = blob.getBinaryStream
    try inputStream.toArray
    finally inputStream.close()
  }
}
