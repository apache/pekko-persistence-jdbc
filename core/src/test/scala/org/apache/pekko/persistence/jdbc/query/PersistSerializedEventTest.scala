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

package org.apache.pekko.persistence.jdbc.query

import java.nio.charset.StandardCharsets

import org.apache.pekko
import pekko.Done
import pekko.actor.{ ActorRef, ActorSystem, Props }
import pekko.pattern.ask
import pekko.persistence.jdbc.journal.dao.JournalTables
import pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
import pekko.persistence.journal.Tagged
import pekko.persistence.query.{ NoOffset, PersistenceQuery }
import pekko.persistence.{ PersistentActor, RecoveryCompleted, SerializedEvent }
import pekko.serialization.{ SerializationExtension, SerializerWithStringManifest, Serializers }
import pekko.stream.scaladsl.Sink

import com.typesafe.config.{ ConfigUtil, ConfigValueFactory }

object PersistSerializedEventTest {
  final case class ManifestEvent(value: String)

  // a serializer with a manifest, Java serialization has none
  class ManifestEventSerializer extends SerializerWithStringManifest {
    override def identifier: Int = 7654321
    override def manifest(o: AnyRef): String = "M"
    override def toBinary(o: AnyRef): Array[Byte] = o match {
      case ManifestEvent(value) => value.getBytes(StandardCharsets.UTF_8)
      case _                    => throw new IllegalArgumentException(s"Unexpected ${o.getClass}")
    }
    override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
      case "M" => ManifestEvent(new String(bytes, StandardCharsets.UTF_8))
      case _   => throw new IllegalArgumentException(s"Unexpected manifest $manifest")
    }
  }

  val serializerConfig = Map(
    "pekko.actor.serializers.manifest-event" ->
    ConfigValueFactory.fromAnyRef(classOf[ManifestEventSerializer].getName),
    ConfigUtil.joinPath("pekko", "actor", "serialization-bindings", classOf[ManifestEvent].getName) ->
    ConfigValueFactory.fromAnyRef("manifest-event"))

  final case class Persist(event: Any)
  case object GetState

  class Persister(override val persistenceId: String) extends PersistentActor {
    private var events = Vector.empty[Any]

    override def receiveCommand: Receive = {
      case Persist(event) =>
        val replyTo = sender()
        persist(event) { _ => replyTo ! Done }
      case GetState =>
        sender() ! events
    }

    override def receiveRecover: Receive = {
      case RecoveryCompleted =>
      case event             => events :+= event
    }
  }
}

abstract class PersistSerializedEventTest(config: String)
    extends QueryTestSpec(config, PersistSerializedEventTest.serializerConfig)
    with JournalTables {
  import PersistSerializedEventTest._

  override val journalTableCfg = journalConfig.eventJournalTableConfiguration
  override val tagTableCfg = journalConfig.eventTagTableConfiguration

  import profile.api._

  case class Row(seqNr: Long, serializerId: Int, manifest: String)

  private def serializedEvent(event: AnyRef)(implicit system: ActorSystem): SerializedEvent = {
    val serializer = SerializationExtension(system).findSerializerFor(event)
    new SerializedEvent(serializer.toBinary(event), serializer.identifier, Serializers.manifestFor(serializer, event))
  }

  private def rows(persistenceId: String): Seq[Row] =
    db.run(
      JournalTable
        .filter(_.persistenceId === persistenceId)
        .sortBy(_.sequenceNumber)
        .map(r => (r.sequenceNumber, r.eventSerId, r.eventSerManifest))
        .result)
      .futureValue
      // Oracle stores an empty string as NULL
      .map { case (seqNr, serId, manifest) => Row(seqNr, serId, Option(manifest).getOrElse("")) }

  private def persister(persistenceId: String)(implicit system: ActorSystem): ActorRef =
    system.actorOf(Props(new Persister(persistenceId)))

  it should "store a SerializedEvent with the given serialization information" in withActorSystem { implicit system =>
    val persistenceId = "serialized-1"

    // String serialization has no manifest
    val serializedEvent1 = serializedEvent("e1")
    serializedEvent1.serializerManifest shouldBe ""

    // ManifestEvent serialization has a manifest
    val serializedEvent2 = serializedEvent(ManifestEvent("e2"))
    serializedEvent2.serializerManifest shouldBe "M"

    val ref = persister(persistenceId)
    (ref ? Persist(serializedEvent1)).futureValue shouldBe Done
    (ref ? Persist(serializedEvent2)).futureValue shouldBe Done
    system.stop(ref)

    rows(persistenceId) shouldBe Seq(
      Row(1L, serializedEvent1.serializerId, serializedEvent1.serializerManifest),
      Row(2L, serializedEvent2.serializerId, serializedEvent2.serializerManifest))

    // replay returns the deserialized events
    val ref2 = persister(persistenceId)
    (ref2 ? GetState).futureValue shouldBe Vector("e1", ManifestEvent("e2"))

    // queries return the deserialized events
    val journalOps = new ScalaJdbcReadJournalOperations(system)
    journalOps.withCurrentEventsByPersistenceId()(persistenceId) { tp =>
      tp.request(Int.MaxValue)
      tp.expectNext().event shouldBe "e1"
      tp.expectNext().event shouldBe ManifestEvent("e2")
      tp.expectComplete()
    }
  }

  it should "keep the tags of a tagged SerializedEvent" in withActorSystem { implicit system =>
    val persistenceId = "serialized-2"
    val serializedEvent1 = serializedEvent("e1")

    val ref = persister(persistenceId)
    (ref ? Persist(Tagged(serializedEvent1, Set("serialized-tag")))).futureValue shouldBe Done

    rows(persistenceId) shouldBe Seq(Row(1L, serializedEvent1.serializerId, serializedEvent1.serializerManifest))

    // the tag query only returns the event once the journal sequence actor has seen its ordering
    val readJournal = PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
    eventually {
      readJournal
        .currentEventsByTag("serialized-tag", NoOffset)
        .runWith(Sink.seq)
        .futureValue
        .map(env => (env.persistenceId, env.event)) shouldBe Seq((persistenceId, "e1"))
    }
  }
}

class H2PersistSerializedEventTest extends PersistSerializedEventTest("h2-application.conf") with H2Cleaner
