CREATE TABLE journal (
  "ordering" BIGINT IDENTITY(1,1) NOT NULL,
  "deleted" BIT DEFAULT 0 NOT NULL,
  "persistence_id" VARCHAR(255) NOT NULL,
  "sequence_number" NUMERIC(10,0) NOT NULL,
  "tags" VARCHAR(255) NULL DEFAULT NULL,
  "message" VARBINARY(max) NOT NULL,
  PRIMARY KEY ("persistence_id", "sequence_number")
)
CREATE UNIQUE INDEX journal_ordering_idx ON journal (ordering);

create table event_tag
(
    "event_id"        BIGINT,
    "persistence_id"  NVARCHAR(255),
    "sequence_number" NUMERIC(10, 0),
    "tag"             NVARCHAR(255) NOT NULL
    primary key ("persistence_id", "sequence_number","tag"),
    constraint "fk_event_journal"
        foreign key ("persistence_id", "sequence_number")
            references "dbo"."event_journal" ("persistence_id", "sequence_number")
            on delete CASCADE
);

CREATE TABLE legacy_snapshot (
  "persistence_id" VARCHAR(255) NOT NULL,
  "sequence_number" NUMERIC(10,0) NOT NULL,
  "created" NUMERIC NOT NULL,
  "snapshot" VARBINARY(max) NOT NULL,
  PRIMARY KEY ("persistence_id", "sequence_number")
);
