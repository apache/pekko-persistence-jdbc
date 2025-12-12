CREATE TABLE event_journal(
    "ordering" BIGINT IDENTITY(1,1) NOT NULL,
    "deleted" BIT DEFAULT 0 NOT NULL,
    "persistence_id" NVARCHAR(255) NOT NULL,
    "sequence_number" NUMERIC(10,0) NOT NULL,
    "writer" NVARCHAR(255) NOT NULL,
    "write_timestamp" BIGINT NOT NULL,
    "adapter_manifest" NVARCHAR(MAX) NOT NULL,
    "event_payload" VARBINARY(MAX) NOT NULL,
    "event_ser_id" INTEGER NOT NULL,
    "event_ser_manifest" NVARCHAR(MAX) NOT NULL,
    "meta_payload" VARBINARY(MAX),
    "meta_ser_id" INTEGER,
    "meta_ser_manifest" NVARCHAR(MAX)
    PRIMARY KEY ("persistence_id", "sequence_number")
);

CREATE UNIQUE INDEX event_journal_ordering_idx ON event_journal(ordering);

CREATE TABLE event_tag (
    "event_id" BIGINT NOT NULL,
    "tag" NVARCHAR(255) NOT NULL
    PRIMARY KEY ("event_id","tag")
    constraint "fk_event_journal"
        foreign key("event_id")
        references "dbo"."event_journal"("ordering")
        on delete CASCADE
);

CREATE TABLE "snapshot" (
    "persistence_id" NVARCHAR(255) NOT NULL,
    "sequence_number" NUMERIC(10,0) NOT NULL,
    "created" BIGINT NOT NULL,
    "snapshot_ser_id" INTEGER NOT NULL,
    "snapshot_ser_manifest" NVARCHAR(255) NOT NULL,
    "snapshot_payload" VARBINARY(MAX) NOT NULL,
    "meta_ser_id" INTEGER,
    "meta_ser_manifest" NVARCHAR(255),
    "meta_payload" VARBINARY(MAX),
    PRIMARY KEY ("persistence_id", "sequence_number")
  )

-- Create Sequence Object
CREATE SEQUENCE global_offset
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE durable_state
(
    "global_offset"         BIGINT
        CONSTRAINT [df_global_offset] DEFAULT
        (NEXT VALUE FOR global_offset),
    "persistence_id"        NVARCHAR(255)  NOT NULL,
    "revision"              NUMERIC(10, 0) NOT NULL,
    "state_payload"         VARBINARY(MAX) NOT NULL,
    "state_serial_id"       INTEGER        NOT NULL,
    "state_serial_manifest" NVARCHAR(MAX),
    "tag"                   NVARCHAR(255),
    "state_timestamp"       BIGINT         NOT NULL
        PRIMARY KEY ("persistence_id")
);
CREATE INDEX durable_state_tag_idx on durable_state (tag);
CREATE UNIQUE INDEX durable_state_global_offset_idx ON durable_state (global_offset);
