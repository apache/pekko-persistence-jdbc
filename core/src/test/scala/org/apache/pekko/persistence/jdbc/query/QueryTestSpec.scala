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
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.query

import org.apache.pekko
import pekko.actor.{ ActorRef, ActorSystem, ExtendedActorSystem, Props, Stash, Status }
import pekko.event.LoggingReceive
import pekko.pattern.ask
import pekko.persistence.jdbc.SingleActorSystemPerTestSpec
import pekko.persistence.jdbc.config.JournalConfig
import pekko.persistence.jdbc.journal.dao.JournalDao
import pekko.persistence.jdbc.query.EventAdapterTest.{ Event, TaggedAsyncEvent, TaggedEvent }
import pekko.persistence.jdbc.query.javadsl.{ JdbcReadJournal => JavaJdbcReadJournal }
import pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
import pekko.persistence.jdbc.testkit.internal._
import pekko.persistence.journal.Tagged
import pekko.persistence.query.{ EventEnvelope, Offset, PersistenceQuery }
import pekko.persistence.{ DeleteMessagesFailure, DeleteMessagesSuccess, PersistentActor }
import pekko.serialization.{ Serialization, SerializationExtension }
import pekko.stream.scaladsl.Sink
import pekko.stream.testkit.TestSubscriber
import pekko.stream.testkit.javadsl.{ TestSink => JavaSink }
import pekko.stream.testkit.scaladsl.TestSink
import pekko.stream.{ Materializer, SystemMaterializer }
import com.typesafe.config.ConfigValue
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.collection.immutable
import scala.concurrent.duration.{ FiniteDuration, _ }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

trait ReadJournalOperations {
  def withCurrentPersistenceIds(within: FiniteDuration = 60.second)(f: TestSubscriber.Probe[String] => Unit): Unit
  def withPersistenceIds(within: FiniteDuration = 60.second)(f: TestSubscriber.Probe[String] => Unit): Unit
  def withCurrentEventsByPersistenceId(within: FiniteDuration = 60.second)(
      persistenceId: String,
      fromSequenceNr: Long = 0,
      toSequenceNr: Long = Long.MaxValue)(f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit
  def withEventsByPersistenceId(within: FiniteDuration = 60.second)(
      persistenceId: String,
      fromSequenceNr: Long = 0,
      toSequenceNr: Long = Long.MaxValue)(f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit
  def withCurrentEventsByTag(within: FiniteDuration = 60.second)(tag: String, offset: Offset)(
      f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit
  def withEventsByTag(within: FiniteDuration = 60.second)(tag: String, offset: Offset)(
      f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit
  def countJournal: Future[Long]
}

class ScalaJdbcReadJournalOperations(readJournal: JdbcReadJournal)(implicit system: ActorSystem, mat: Materializer)
    extends ReadJournalOperations {
  def this(system: ActorSystem) =
    this(PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier))(
      system,
      SystemMaterializer(system).materializer)

  import system.dispatcher

  def withCurrentPersistenceIds(within: FiniteDuration)(f: TestSubscriber.Probe[String] => Unit): Unit = {
    val tp = readJournal.currentPersistenceIds().runWith(TestSink.probe[String])
    tp.within(within)(f(tp))
  }

  def withPersistenceIds(within: FiniteDuration)(f: TestSubscriber.Probe[String] => Unit): Unit = {
    val tp = readJournal.persistenceIds().runWith(TestSink.probe[String])
    tp.within(within)(f(tp))
  }

  def withCurrentEventsByPersistenceId(
      within: FiniteDuration)(persistenceId: String, fromSequenceNr: Long = 0, toSequenceNr: Long = Long.MaxValue)(
      f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit = {
    val tp = readJournal
      .currentEventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr)
      .runWith(TestSink.probe[EventEnvelope])
    tp.within(within)(f(tp))
  }

  def withEventsByPersistenceId(
      within: FiniteDuration)(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long)(
      f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit = {
    val tp = readJournal
      .eventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr)
      .runWith(TestSink.probe[EventEnvelope])
    tp.within(within)(f(tp))
  }

  def withCurrentEventsByTag(within: FiniteDuration)(tag: String, offset: Offset)(
      f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit = {
    val tp = readJournal.currentEventsByTag(tag, offset).runWith(TestSink.probe[EventEnvelope])
    tp.within(within)(f(tp))
  }

  def withEventsByTag(within: FiniteDuration)(tag: String, offset: Offset)(
      f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit = {
    val tp = readJournal.eventsByTag(tag, offset).runWith(TestSink.probe[EventEnvelope])
    tp.within(within)(f(tp))
  }

  def currentLastKnownSequenceNumberByPersistenceId(
      persistenceId: String
  ): Future[Option[Long]] = {

    readJournal.currentLastKnownSequenceNumberByPersistenceId(persistenceId)
  }

  override def countJournal: Future[Long] =
    readJournal
      .currentPersistenceIds()
      .filter(pid => (1 to 3).map(id => s"my-$id").contains(pid))
      .mapAsync(1) { pid =>
        readJournal.currentEventsByPersistenceId(pid, 0, Long.MaxValue).map(_ => 1L).runWith(Sink.seq).map(_.sum)
      }
      .runWith(Sink.seq)
      .map(_.sum)
}

class JavaDslJdbcReadJournalOperations(readJournal: javadsl.JdbcReadJournal)(
    implicit system: ActorSystem,
    mat: Materializer)
    extends ReadJournalOperations {
  def this(system: ActorSystem) =
    this(
      PersistenceQuery.get(system).getReadJournalFor(classOf[javadsl.JdbcReadJournal], JavaJdbcReadJournal.Identifier))(
      system,
      SystemMaterializer(system).materializer)

  import system.dispatcher

  def withCurrentPersistenceIds(within: FiniteDuration)(f: TestSubscriber.Probe[String] => Unit): Unit = {
    val sink: pekko.stream.javadsl.Sink[String, TestSubscriber.Probe[String]] = JavaSink.probe(system)
    val tp = readJournal.currentPersistenceIds().runWith(sink, mat)
    tp.within(within)(f(tp))
  }

  def withPersistenceIds(within: FiniteDuration)(f: TestSubscriber.Probe[String] => Unit): Unit = {
    val sink: pekko.stream.javadsl.Sink[String, TestSubscriber.Probe[String]] = JavaSink.probe(system)
    val tp = readJournal.persistenceIds().runWith(sink, mat)
    tp.within(within)(f(tp))
  }

  def withCurrentEventsByPersistenceId(
      within: FiniteDuration)(persistenceId: String, fromSequenceNr: Long = 0, toSequenceNr: Long = Long.MaxValue)(
      f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit = {
    val sink: pekko.stream.javadsl.Sink[EventEnvelope, TestSubscriber.Probe[EventEnvelope]] =
      JavaSink.probe(system)
    val tp = readJournal.currentEventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr).runWith(sink, mat)
    tp.within(within)(f(tp))
  }

  def withEventsByPersistenceId(
      within: FiniteDuration)(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long)(
      f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit = {
    val sink: pekko.stream.javadsl.Sink[EventEnvelope, TestSubscriber.Probe[EventEnvelope]] =
      JavaSink.probe(system)
    val tp = readJournal.eventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr).runWith(sink, mat)
    tp.within(within)(f(tp))
  }

  def withCurrentEventsByTag(within: FiniteDuration)(tag: String, offset: Offset)(
      f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit = {
    val sink: pekko.stream.javadsl.Sink[EventEnvelope, TestSubscriber.Probe[EventEnvelope]] =
      JavaSink.probe(system)
    val tp = readJournal.currentEventsByTag(tag, offset).runWith(sink, mat)
    tp.within(within)(f(tp))
  }

  def withEventsByTag(within: FiniteDuration)(tag: String, offset: Offset)(
      f: TestSubscriber.Probe[EventEnvelope] => Unit): Unit = {
    val sink: pekko.stream.javadsl.Sink[EventEnvelope, TestSubscriber.Probe[EventEnvelope]] =
      JavaSink.probe(system)
    val tp = readJournal.eventsByTag(tag, offset).runWith(sink, mat)
    tp.within(within)(f(tp))
  }

  override def countJournal: Future[Long] =
    readJournal
      .currentPersistenceIds()
      .asScala
      .filter(pid => (1 to 3).map(id => s"my-$id").contains(pid))
      .mapAsync(1) { pid =>
        readJournal
          .currentEventsByPersistenceId(pid, 0, Long.MaxValue)
          .asScala
          .map(_ => 1L)
          .runFold(List.empty[Long])(_ :+ _)
          .map(_.sum)
      }
      .runFold(List.empty[Long])(_ :+ _)
      .map(_.sum)
}

object QueryTestSpec {
  implicit final class EventEnvelopeProbeOps(val probe: TestSubscriber.Probe[EventEnvelope]) extends AnyVal {
    def expectNextEventEnvelope(
        persistenceId: String,
        sequenceNr: Long,
        event: Any): TestSubscriber.Probe[EventEnvelope] = {
      val env = probe.expectNext()
      assertEnvelope(env, persistenceId, sequenceNr, event)
      probe
    }

    def expectNextEventEnvelope(
        timeout: FiniteDuration,
        persistenceId: String,
        sequenceNr: Long,
        event: Any): TestSubscriber.Probe[EventEnvelope] = {
      val env = probe.expectNext(timeout)
      assertEnvelope(env, persistenceId, sequenceNr, event)
      probe
    }

    private def assertEnvelope(env: EventEnvelope, persistenceId: String, sequenceNr: Long, event: Any): Unit = {
      assert(
        env.persistenceId == persistenceId,
        s"expected persistenceId $persistenceId, found ${env.persistenceId}, in $env")
      assert(env.sequenceNr == sequenceNr, s"expected sequenceNr $sequenceNr, found ${env.sequenceNr}, in $env")
      assert(env.event == event, s"expected event $event, found ${env.event}, in $env")
    }
  }
}

abstract class QueryTestSpec(config: String, configOverrides: Map[String, ConfigValue] = Map.empty)
    extends SingleActorSystemPerTestSpec(config, configOverrides) {
  case class DeleteCmd(toSequenceNr: Long = Long.MaxValue) extends Serializable

  final val ExpectNextTimeout = 10.second

  class TestActor(id: Int, replyToMessages: Boolean) extends PersistentActor with Stash {
    override val persistenceId: String = "my-" + id

    var state: Int = 0

    override def receiveCommand: Receive = idle

    def idle: Receive =
      LoggingReceive {
        case "state" =>
          sender() ! state

        case DeleteCmd(toSequenceNr) =>
          deleteMessages(toSequenceNr)
          if (replyToMessages) {
            context.become(awaitingDeleting(sender()))
          }

        case event: Int =>
          persist(event) { (event: Int) =>
            updateState(event)
            if (replyToMessages) sender() ! pekko.actor.Status.Success(event)
          }

        case event @ Tagged(payload: Int, tags) =>
          persist(event) { _ =>
            updateState(payload)
            if (replyToMessages) sender() ! pekko.actor.Status.Success((payload, tags))
          }
        case event: Event =>
          persist(event) { evt =>
            if (replyToMessages) sender() ! pekko.actor.Status.Success(evt)
          }

        case event @ TaggedEvent(payload: Event, tag) =>
          persist(event) { _ =>
            if (replyToMessages) sender() ! pekko.actor.Status.Success((payload, tag))
          }
        case event @ TaggedAsyncEvent(payload: Event, tag) =>
          persistAsync(event) { _ =>
            if (replyToMessages) sender() ! pekko.actor.Status.Success((payload, tag))
          }
      }

    def awaitingDeleting(origSender: ActorRef): Receive =
      LoggingReceive {
        case DeleteMessagesSuccess(toSequenceNr) =>
          origSender ! s"deleted-$toSequenceNr"
          unstashAll()
          context.become(idle)

        case DeleteMessagesFailure(ex, _) =>
          origSender ! Status.Failure(ex)
          unstashAll()
          context.become(idle)

        // stash whatever other messages
        case _ => stash()
      }

    def updateState(event: Int): Unit = {
      state = state + event
    }

    override def receiveRecover: Receive =
      LoggingReceive { case event: Int =>
        updateState(event)
      }
  }

  def pendingIfOracleWithLegacy(): Unit = {
    if (profile == slick.jdbc.OracleProfile && readJournalConfig.pluginConfig.dao == classOf[
        pekko.persistence.jdbc.query.dao.legacy.ByteArrayReadJournalDao].getName)
      pending // TODO https://github.com/akka/akka-persistence-jdbc/issues/673
  }

  def setupEmpty(persistenceId: Int, replyToMessages: Boolean)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(new TestActor(persistenceId, replyToMessages)))
  }

  def withTestActors(seq: Int = 1, replyToMessages: Boolean = false)(f: (ActorRef, ActorRef, ActorRef) => Unit)(
      implicit system: ActorSystem): Unit = {
    val refs = (seq until seq + 3).map(setupEmpty(_, replyToMessages)).toList
    try {
      expectAllStarted(refs)
      f(refs.head, refs.drop(1).head, refs.drop(2).head)
    } finally killActors(refs: _*)
  }

  def withManyTestActors(amount: Int, seq: Int = 1, replyToMessages: Boolean = false)(f: Seq[ActorRef] => Unit)(
      implicit system: ActorSystem): Unit = {
    val refs = (seq until seq + amount).map(setupEmpty(_, replyToMessages)).toList
    try {
      expectAllStarted(refs)
      f(refs)
    } finally killActors(refs: _*)
  }

  def expectAllStarted(refs: Seq[ActorRef])(implicit system: ActorSystem): Unit = {
    // make sure we notice early if the actors failed to start (because of issues with journal) makes debugging
    // failing tests easier as we know it is not the actual interaction from the test that is the problem
    implicit val ec: ExecutionContext = system.dispatcher
    Future.sequence(refs.map(_ ? "state")).futureValue
  }

  def withTags(payload: Any, tags: String*) = Tagged(payload, Set(tags: _*))

  def withDao(f: JournalDao => Unit)(implicit system: ActorSystem, ec: ExecutionContext, mat: Materializer): Unit = {
    val fqcn: String = journalConfig.pluginConfig.dao
    val args: immutable.Seq[(Class[_], AnyRef)] = immutable.Seq(
      (classOf[Database], db), (classOf[JdbcProfile], profile), (classOf[JournalConfig], journalConfig),
      (classOf[Serialization], SerializationExtension(system)), (classOf[ExecutionContext], ec),
      (classOf[Materializer], mat))
    val journalDao: JournalDao =
      system.asInstanceOf[ExtendedActorSystem].dynamicAccess.createInstanceFor[JournalDao](fqcn, args) match {
        case Success(dao)   => dao
        case Failure(cause) => throw cause
      }
    f(journalDao)
  }

}

trait PostgresCleaner extends QueryTestSpec {

  def clearPostgres(): Unit =
    tables.foreach { name => withStatement(stmt => stmt.executeUpdate(s"DELETE FROM $name")) }

  override def beforeAll(): Unit = {
    dropAndCreate(Postgres)
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    dropAndCreate(Postgres)
    super.beforeEach()
  }
}

trait MysqlCleaner extends QueryTestSpec {

  def clearMySQL(): Unit = {
    withStatement { stmt =>
      stmt.execute("SET FOREIGN_KEY_CHECKS = 0")
      tables.foreach { name => stmt.executeUpdate(s"TRUNCATE $name") }
      stmt.execute("SET FOREIGN_KEY_CHECKS = 1")
    }
  }

  override def beforeAll(): Unit = {
    dropAndCreate(MySQL)
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    clearMySQL()
    super.beforeEach()
  }
}

trait OracleCleaner extends QueryTestSpec {

  def clearOracle(): Unit = {
    tables.foreach { name =>
      withStatement(stmt => stmt.executeUpdate(s"""DELETE FROM "$name" """))
    }
    withStatement(stmt => stmt.executeUpdate("""BEGIN "reset_sequence"; END; """))
  }

  override def beforeAll(): Unit = {
    dropAndCreate(Oracle)
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    clearOracle()
    super.beforeEach()
  }
}

trait SqlServerCleaner extends QueryTestSpec {

  var initial = true

  def clearSqlServer(): Unit = {
    val reset = if (initial) {
      initial = false
      1
    } else {
      0
    }
    withStatement { stmt =>
      tables.foreach { name => stmt.executeUpdate(s"DELETE FROM $name") }
      stmt.executeUpdate(s"DBCC CHECKIDENT('$journalTableName', RESEED, $reset)")
    }
  }

  override def beforeAll() = {
    dropAndCreate(SqlServer)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    dropAndCreate(SqlServer)
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    clearSqlServer()
    super.beforeEach()
  }
}

trait H2Cleaner extends QueryTestSpec {

  def clearH2(): Unit =
    tables.foreach { name => withStatement(stmt => stmt.executeUpdate(s"DELETE FROM $name")) }

  override def beforeEach(): Unit = {
    dropAndCreate(H2)
    super.beforeEach()
  }
}
