CREATE TABLE IF NOT EXISTS public.journal (
  ordering BIGSERIAL,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  deleted BOOLEAN DEFAULT FALSE NOT NULL,
  tags VARCHAR(255) DEFAULT NULL,
  message BYTEA NOT NULL,
  PRIMARY KEY(persistence_id, sequence_number)
);
CREATE UNIQUE INDEX IF NOT EXISTS journal_ordering_idx ON public.journal(ordering);

CREATE TABLE IF NOT EXISTS public.event_tag (
    event_id BIGINT,
    persistence_id VARCHAR(255),
    sequence_number BIGINT,
    tag VARCHAR(256),
    PRIMARY KEY(persistence_id, sequence_number, tag),
    CONSTRAINT fk_event_journal
    FOREIGN KEY(persistence_id, sequence_number)
    REFERENCES event_journal(persistence_id, sequence_number)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.legacy_snapshot (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  created BIGINT NOT NULL,
  snapshot BYTEA NOT NULL,
  PRIMARY KEY(persistence_id, sequence_number)
);

CREATE TABLE IF NOT EXISTS public.durable_state (
    global_offset BIGSERIAL,
    persistence_id VARCHAR(255) NOT NULL,
    revision BIGINT NOT NULL,
    state_payload BYTEA NOT NULL,
    state_serial_id INTEGER NOT NULL,
    state_serial_manifest VARCHAR(255),
    tag VARCHAR,
    state_timestamp BIGINT NOT NULL,
    PRIMARY KEY(persistence_id)
    );
CREATE INDEX CONCURRENTLY state_tag_idx on public.durable_state (tag);
CREATE INDEX CONCURRENTLY state_global_offset_idx on public.durable_state (global_offset);
