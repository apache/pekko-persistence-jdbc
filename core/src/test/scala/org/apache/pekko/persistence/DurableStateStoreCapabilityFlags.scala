/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) 2014-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence

//#durable-state-store-flags
trait DurableStateStoreCapabilityFlags {

  /**
   * When `true` enables tests which check if the durable state store properly rejects
   * a `deleteObject` call when the revision does not match the stored revision.
   */
  protected def supportsDeleteWithRevisionCheck: CapabilityFlag

  /**
   * When `true` enables tests which check if the durable state store properly rejects
   * an `upsertObject` call when the revision is stale (does not match the expected next revision).
   * This is the optimistic concurrency check on writes.
   */
  protected def supportsUpsertWithRevisionCheck: CapabilityFlag

  /**
   * When `true` enables tests which check if the durable state store properly serializes
   * and deserializes values via the configured Pekko serializer infrastructure.
   */
  protected def supportsSerialization: CapabilityFlag

  /**
   * When `true` enables tests which check the deprecated single-argument `deleteObject(persistenceId)`
   * overload, which deletes regardless of the current revision.
   */
  protected def supportsSoftDelete: CapabilityFlag
}
//#durable-state-store-flags
