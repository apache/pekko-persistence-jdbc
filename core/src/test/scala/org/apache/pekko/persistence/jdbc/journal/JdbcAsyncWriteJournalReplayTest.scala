/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2023 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.journal

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.Scheduler
import pekko.persistence.{ JournalProtocol, Persistence, PersistentRepr }
import pekko.persistence.jdbc.SingleActorSystemPerTestSpec
import pekko.persistence.jdbc.config.JournalConfig
import pekko.persistence.jdbc.journal.dao.DefaultJournalDao
import pekko.serialization.Serialization
import pekko.stream.Materializer
import pekko.stream.scaladsl.Source
import pekko.testkit.TestProbe
import com.typesafe.config.ConfigValueFactory
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

/**
 * A journal dao whose replay stream contains a failed element between two successful ones.
 */
class FailingReplayJournalDao(
    db: Database,
    profile: JdbcProfile,
    journalConfig: JournalConfig,
    serialization: Serialization)(implicit ec: ExecutionContext, mat: Materializer)
    extends DefaultJournalDao(db, profile, journalConfig, serialization) {

  override def highestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] =
    Future.successful(3L)

  override def messagesWithBatch(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      batchSize: Int,
      refreshInterval: Option[(FiniteDuration, Scheduler)]): Source[Try[(PersistentRepr, Long)], NotUsed] =
    Source(
      List(
        Success((PersistentRepr("event-1", 1L, persistenceId), 1L)),
        Failure(FailingReplayJournalDao.ReplayFailure),
        Success((PersistentRepr("event-3", 3L, persistenceId), 3L))))
}

object FailingReplayJournalDao {
  object ReplayFailure extends RuntimeException("deserialization failed") with scala.util.control.NoStackTrace
}

class JdbcAsyncWriteJournalReplayTest
    extends SingleActorSystemPerTestSpec(
      "h2-shared-db-application.conf",
      Map("jdbc-journal.dao" -> ConfigValueFactory.fromAnyRef(classOf[FailingReplayJournalDao].getName))) {

  it should "fail the replay at the first failed element and not replay the elements after it" in {
    withActorSystem { implicit system =>
      val journal = Persistence(system).journalFor("jdbc-journal")
      val probe = TestProbe()

      journal ! JournalProtocol.ReplayMessages(1L, Long.MaxValue, Long.MaxValue, "pid-1", probe.ref)

      probe.expectMsgType[JournalProtocol.ReplayedMessage].persistent.payload shouldBe "event-1"
      probe.expectMsgType[JournalProtocol.ReplayMessagesFailure].cause shouldBe FailingReplayJournalDao.ReplayFailure
      probe.expectNoMessage(100.millis)
    }
  }
}
