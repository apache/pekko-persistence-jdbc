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

package org.apache.pekko.persistence.jdbc;

import java.util.concurrent.CompletionStage;
import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
// #create
import org.apache.pekko.persistence.jdbc.query.javadsl.JdbcReadJournal;
// #read-journal
// #persistence-ids
import org.apache.pekko.persistence.jdbc.testkit.javadsl.SchemaUtils;
// #create
// #read-journal
import org.apache.pekko.persistence.query.*;
import org.apache.pekko.persistence.query.EventEnvelope;
import org.apache.pekko.persistence.query.PersistenceQuery;
import org.apache.pekko.stream.javadsl.Source;

final class JavadslSnippets {
  void create() {
    // #create

    ActorSystem actorSystem = ActorSystem.create("example");
    CompletionStage<Done> done = SchemaUtils.createIfNotExists(actorSystem);
    // #create
  }

  void readJournal() {
    ActorSystem system = ActorSystem.create("example");
    // #read-journal

    final JdbcReadJournal readJournal =
        PersistenceQuery.get(system)
            .getReadJournalFor(JdbcReadJournal.class, JdbcReadJournal.Identifier());
    // #read-journal

  }

  void persistenceIds() {
    ActorSystem system = ActorSystem.create();
    // #persistence-ids

    JdbcReadJournal readJournal =
        PersistenceQuery.get(system)
            .getReadJournalFor(JdbcReadJournal.class, JdbcReadJournal.Identifier());

    Source<String, NotUsed> willNotCompleteTheStream = readJournal.persistenceIds();

    Source<String, NotUsed> willCompleteTheStream = readJournal.currentPersistenceIds();
    // #persistence-ids
  }

  void eventsByPersistenceIds() {
    ActorSystem system = ActorSystem.create();

    // #events-by-persistence-id

    JdbcReadJournal readJournal =
        PersistenceQuery.get(system)
            .getReadJournalFor(JdbcReadJournal.class, JdbcReadJournal.Identifier());

    Source<EventEnvelope, NotUsed> willNotCompleteTheStream =
        readJournal.eventsByPersistenceId("some-persistence-id", 0L, Long.MAX_VALUE);

    Source<EventEnvelope, NotUsed> willCompleteTheStream =
        readJournal.currentEventsByPersistenceId("some-persistence-id", 0L, Long.MAX_VALUE);
    // #events-by-persistence-id
  }

  void eventsByTag() {
    ActorSystem system = ActorSystem.create();
    // #events-by-tag

    JdbcReadJournal readJournal =
        PersistenceQuery.get(system)
            .getReadJournalFor(JdbcReadJournal.class, JdbcReadJournal.Identifier());

    Source<EventEnvelope, NotUsed> willNotCompleteTheStream =
        readJournal.eventsByTag("apple", Offset.sequence(0L));

    Source<EventEnvelope, NotUsed> willCompleteTheStream =
        readJournal.currentEventsByTag("apple", Offset.sequence(0L));
    // #events-by-tag
  }
}
