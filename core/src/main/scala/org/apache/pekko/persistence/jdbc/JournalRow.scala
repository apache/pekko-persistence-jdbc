/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc

final case class JournalRow(
    ordering: Long,
    deleted: Boolean,
    persistenceId: String,
    sequenceNumber: Long,
    message: Array[Byte],
    tags: Option[String] = None)
