/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.persistence.jdbc.state;

import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
// #create
import org.apache.pekko.persistence.jdbc.testkit.javadsl.SchemaUtils;
// #create
// #jdbc-durable-state-store
import org.apache.pekko.persistence.state.DurableStateStoreRegistry;
import org.apache.pekko.persistence.jdbc.state.javadsl.JdbcDurableStateStore;
// #jdbc-durable-state-store
// #get-object
import org.apache.pekko.persistence.state.DurableStateStoreRegistry;
import org.apache.pekko.persistence.jdbc.state.javadsl.JdbcDurableStateStore;
import org.apache.pekko.persistence.state.javadsl.GetObjectResult;
// #get-object
// #upsert-get-object
import org.apache.pekko.persistence.state.DurableStateStoreRegistry;
import org.apache.pekko.persistence.jdbc.state.javadsl.JdbcDurableStateStore;
import org.apache.pekko.persistence.state.javadsl.GetObjectResult;
// #upsert-get-object
// #delete-object
import org.apache.pekko.persistence.state.DurableStateStoreRegistry;
import org.apache.pekko.persistence.jdbc.state.javadsl.JdbcDurableStateStore;
// #delete-object
// #current-changes
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.persistence.state.DurableStateStoreRegistry;
import org.apache.pekko.persistence.jdbc.state.javadsl.JdbcDurableStateStore;
import org.apache.pekko.persistence.query.DurableStateChange;
import org.apache.pekko.persistence.query.NoOffset;
// #current-changes
// #changes
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.persistence.state.DurableStateStoreRegistry;
import org.apache.pekko.persistence.jdbc.state.javadsl.JdbcDurableStateStore;
import org.apache.pekko.persistence.query.DurableStateChange;
import org.apache.pekko.persistence.query.NoOffset;
// #changes

final class JavadslSnippets {
  void create() {
    // #create

    ActorSystem system = ActorSystem.create("example");
    CompletionStage<Done> done = SchemaUtils.createIfNotExists(system);
    // #create
  }

  void durableStatePlugin() {
    ActorSystem system = ActorSystem.create("example");

    // #jdbc-durable-state-store

    @SuppressWarnings("unchecked")
    JdbcDurableStateStore<String> store =
        DurableStateStoreRegistry.get(system)
            .getDurableStateStoreFor(
                JdbcDurableStateStore.class, JdbcDurableStateStore.Identifier());
    // #jdbc-durable-state-store
  }

  void getObject() {
    ActorSystem system = ActorSystem.create("example");

    // #get-object

    @SuppressWarnings("unchecked")
    JdbcDurableStateStore<String> store =
        DurableStateStoreRegistry.get(system)
            .getDurableStateStoreFor(
                JdbcDurableStateStore.class, JdbcDurableStateStore.Identifier());

    CompletionStage<GetObjectResult<String>> futureResult = store.getObject("InvalidPersistenceId");
    try {
      GetObjectResult<String> result = futureResult.toCompletableFuture().get();
      assert !result.value().isPresent();
    } catch (Exception e) {
      // handle exceptions
    }
    // #get-object
  }

  void upsertAndGetObject() {
    ActorSystem system = ActorSystem.create("example");

    // #upsert-get-object

    @SuppressWarnings("unchecked")
    JdbcDurableStateStore<String> store =
        DurableStateStoreRegistry.get(system)
            .getDurableStateStoreFor(
                JdbcDurableStateStore.class, JdbcDurableStateStore.Identifier());

    CompletionStage<GetObjectResult<String>> r =
        store
            .upsertObject("p234", 1, "a valid string", "t123")
            .thenCompose(d -> store.getObject("p234"))
            .thenCompose(o -> store.upsertObject("p234", 2, "updated valid string", "t123"))
            .thenCompose(d -> store.getObject("p234"));

    try {
      assert r.toCompletableFuture().get().value().get().equals("updated valid string");
    } catch (Exception e) {
      // handle exceptions
    }
    // #upsert-get-object
  }

  void deleteObject() {
    ActorSystem system = ActorSystem.create("example");

    // #delete-object

    @SuppressWarnings("unchecked")
    JdbcDurableStateStore<String> store =
        DurableStateStoreRegistry.get(system)
            .getDurableStateStoreFor(
                JdbcDurableStateStore.class, JdbcDurableStateStore.Identifier());

    CompletionStage<Done> futureResult = store.deleteObject("p123");
    try {
      assert futureResult.toCompletableFuture().get().equals(Done.getInstance());
    } catch (Exception e) {
      // handle exceptions
    }
    // #delete-object
  }

  void currentChanges() {
    ActorSystem system = ActorSystem.create("example");

    // #current-changes

    @SuppressWarnings("unchecked")
    JdbcDurableStateStore<String> store =
        DurableStateStoreRegistry.get(system)
            .getDurableStateStoreFor(
                JdbcDurableStateStore.class, JdbcDurableStateStore.Identifier());

    Source<DurableStateChange<String>, NotUsed> willCompleteTheStream =
        store.currentChanges("tag-1", NoOffset.getInstance());
    // #current-changes
  }

  void changes() {
    ActorSystem system = ActorSystem.create("example");

    // #changes

    @SuppressWarnings("unchecked")
    JdbcDurableStateStore<String> store =
        DurableStateStoreRegistry.get(system)
            .getDurableStateStoreFor(
                JdbcDurableStateStore.class, JdbcDurableStateStore.Identifier());

    Source<DurableStateChange<String>, NotUsed> willNotCompleteTheStream =
        store.changes("tag-1", NoOffset.getInstance());
    // #changes
  }
}
