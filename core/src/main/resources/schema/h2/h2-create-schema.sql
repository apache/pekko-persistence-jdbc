CREATE TABLE IF NOT EXISTS PUBLIC."event_journal" (
    "ordering" BIGINT UNIQUE NOT NULL AUTO_INCREMENT,
    "deleted" BOOLEAN DEFAULT false NOT NULL,
    "persistence_id" VARCHAR(255) NOT NULL,
    "sequence_number" BIGINT NOT NULL,
    "writer" VARCHAR NOT NULL,
    "write_timestamp" BIGINT NOT NULL,
    "adapter_manifest" VARCHAR NOT NULL,
    "event_payload" BLOB NOT NULL,
    "event_ser_id" INTEGER NOT NULL,
    "event_ser_manifest" VARCHAR NOT NULL,
    "meta_payload" BLOB,
    "meta_ser_id" INTEGER,
    "meta_ser_manifest" VARCHAR,
    PRIMARY KEY("persistence_id","sequence_number")
    );

CREATE UNIQUE INDEX "event_journal_ordering_idx" on PUBLIC."event_journal" ("ordering");

CREATE TABLE IF NOT EXISTS PUBLIC."event_tag" (
    "event_id" BIGINT,
    "persistence_id" VARCHAR(255),
    "sequence_number" BIGINT,
    "tag" VARCHAR NOT NULL,
    PRIMARY KEY("persistence_id", "sequence_number", "tag"),
    CONSTRAINT fk_event_journal
      FOREIGN KEY("persistence_id", "sequence_number")
      REFERENCES "event_journal"("persistence_id", "sequence_number")
      ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS PUBLIC."snapshot" (
    "persistence_id" VARCHAR(255) NOT NULL,
    "sequence_number" BIGINT NOT NULL,
    "created" BIGINT NOT NULL,"snapshot_ser_id" INTEGER NOT NULL,
    "snapshot_ser_manifest" VARCHAR NOT NULL,
    "snapshot_payload" BLOB NOT NULL,
    "meta_ser_id" INTEGER,
    "meta_ser_manifest" VARCHAR,
    "meta_payload" BLOB,
    PRIMARY KEY("persistence_id","sequence_number")
    );

CREATE SEQUENCE IF NOT EXISTS PUBLIC."global_offset_seq";

CREATE TABLE IF NOT EXISTS PUBLIC."durable_state" (
    "global_offset" BIGINT DEFAULT NEXT VALUE FOR PUBLIC."global_offset_seq",
    "persistence_id" VARCHAR(255) NOT NULL,
    "revision" BIGINT NOT NULL,
    "state_payload" BLOB NOT NULL,
    "state_serial_id" INTEGER NOT NULL,
    "state_serial_manifest" VARCHAR,
    "tag" VARCHAR,
    "state_timestamp" BIGINT NOT NULL,
    PRIMARY KEY("persistence_id")
    );
CREATE INDEX IF NOT EXISTS "state_tag_idx" on PUBLIC."durable_state" ("tag");
CREATE INDEX IF NOT EXISTS "state_global_offset_idx" on PUBLIC."durable_state" ("global_offset");
