/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.journal

import java.util.{ HashMap => JHMap, Map => JMap }

import org.apache.pekko
import pekko.Done
import pekko.actor.{ ActorSystem, ExtendedActorSystem }
import pekko.persistence.jdbc.config.JournalConfig
import pekko.persistence.jdbc.journal.JdbcAsyncWriteJournal.{ InPlaceUpdateEvent, WriteFinished }
import pekko.persistence.jdbc.journal.dao.{ JournalDao, JournalDaoWithUpdates }
import pekko.persistence.jdbc.db.{ SlickDatabase, SlickExtension }
import pekko.persistence.journal.AsyncWriteJournal
import pekko.persistence.{ AtomicWrite, PersistentRepr }
import pekko.serialization.{ Serialization, SerializationExtension }
import pekko.stream.{ Materializer, SystemMaterializer }
import com.typesafe.config.Config
import slick.jdbc.JdbcProfile
import slick.jdbc.JdbcBackend._

import scala.collection.immutable._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }
import pekko.pattern.pipe
import pekko.persistence.jdbc.util.PluginVersionChecker

object JdbcAsyncWriteJournal {
  private case class WriteFinished(pid: String, f: Future[_])

  /**
   * Extra Plugin API: May be used to issue in-place updates for events.
   * To be used only for data migrations such as "encrypt all events" and similar operations.
   *
   * The write payload may be wrapped in a [[pekko.persistence.journal.Tagged]],
   * in which case the new tags will overwrite the existing tags of the event.
   */
  final case class InPlaceUpdateEvent(persistenceId: String, seqNr: Long, write: AnyRef)
}

class JdbcAsyncWriteJournal(config: Config) extends AsyncWriteJournal {

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val system: ActorSystem = context.system
  implicit val mat: Materializer = SystemMaterializer(system).materializer
  val journalConfig = new JournalConfig(config)

  PluginVersionChecker.check()

  val slickDb: SlickDatabase = SlickExtension(system).database(config)
  def db: Database = slickDb.database

  val journalDao: JournalDao = {
    val fqcn = journalConfig.pluginConfig.dao
    val profile: JdbcProfile = slickDb.profile
    val args = Seq(
      (classOf[Database], db),
      (classOf[JdbcProfile], profile),
      (classOf[JournalConfig], journalConfig),
      (classOf[Serialization], SerializationExtension(system)),
      (classOf[ExecutionContext], ec),
      (classOf[Materializer], mat))
    system.asInstanceOf[ExtendedActorSystem].dynamicAccess.createInstanceFor[JournalDao](fqcn, args) match {
      case Success(dao)   => dao
      case Failure(cause) => throw cause
    }
  }
  // only accessed if we need to perform Updates -- which is very rarely
  def journalDaoWithUpdates: JournalDaoWithUpdates =
    journalDao match {
      case upgraded: JournalDaoWithUpdates => upgraded
      case _ =>
        throw new IllegalStateException(s"The ${journalDao.getClass} does NOT implement [JournalDaoWithUpdates], " +
          s"which is required to perform updates of events! Please configure a valid update capable DAO (e.g. the default [ByteArrayJournalDao].")
    }

  // readHighestSequence must be performed after pending write for a persistenceId
  // when the persistent actor is restarted.
  private val writeInProgress: JMap[String, Future[_]] = new JHMap

  override def asyncWriteMessages(messages: Seq[AtomicWrite]): Future[Seq[Try[Unit]]] = {
    // add timestamp to all payloads in all AtomicWrite messages
    val now = System.currentTimeMillis()
    val timedMessages =
      messages.map { atomWrt =>
        atomWrt.copy(payload = atomWrt.payload.map(pr => pr.withTimestamp(now)))
      }

    val future = journalDao.asyncWriteMessages(timedMessages)
    val persistenceId = timedMessages.head.persistenceId
    writeInProgress.put(persistenceId, future)
    future.onComplete(_ => self ! WriteFinished(persistenceId, future))
    future
  }

  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long): Future[Unit] =
    journalDao.delete(persistenceId, toSequenceNr)

  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = {
    def fetchHighestSeqNr() = journalDao.highestSequenceNr(persistenceId, fromSequenceNr)
    writeInProgress.get(persistenceId) match {
      case null => fetchHighestSeqNr()
      case f    =>
        // we must fetch the highest sequence number after the previous write has completed
        // If the previous write failed then we can ignore this
        f.recover { case _ => () }.flatMap(_ => fetchHighestSeqNr())
    }
  }

  private def asyncUpdateEvent(persistenceId: String, sequenceNr: Long, message: AnyRef): Future[Done] = {
    journalDaoWithUpdates.update(persistenceId, sequenceNr, message)
  }

  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(
      recoveryCallback: (PersistentRepr) => Unit): Future[Unit] =
    journalDao
      .messagesWithBatch(persistenceId, fromSequenceNr, toSequenceNr, journalConfig.daoConfig.replayBatchSize, None)
      .take(max)
      .mapAsync(1)(reprAndOrdNr => Future.fromTry(reprAndOrdNr))
      .runForeach { case (repr, _) =>
        recoveryCallback(repr)
      }
      .map(_ => ())

  override def postStop(): Unit = {
    if (slickDb.allowShutdown) {
      // Since a (new) db is created when this actor (re)starts, we must close it when the actor stops
      db.close()
    }
    super.postStop()
  }

  override def receivePluginInternal: Receive = {
    case WriteFinished(persistenceId, future) =>
      writeInProgress.remove(persistenceId, future)
    case InPlaceUpdateEvent(pid, seq, write) =>
      asyncUpdateEvent(pid, seq, write).pipeTo(sender())
  }
}
