CREATE TABLE IF NOT EXISTS event_journal
(
    ordering           SERIAL,
    deleted            BOOLEAN DEFAULT false NOT NULL,
    persistence_id     VARCHAR(255)          NOT NULL,
    sequence_number    BIGINT                NOT NULL,
    writer             TEXT                  NOT NULL,
    write_timestamp    BIGINT                NOT NULL,
    adapter_manifest   TEXT                  NOT NULL,
    event_payload      BLOB                  NOT NULL,
    event_ser_id       INTEGER               NOT NULL,
    event_ser_manifest TEXT                  NOT NULL,
    meta_payload       BLOB,
    meta_ser_id        INTEGER,
    meta_ser_manifest  TEXT,
    PRIMARY KEY (persistence_id, sequence_number)
);

CREATE UNIQUE INDEX event_journal_ordering_idx ON event_journal (ordering);

CREATE TABLE IF NOT EXISTS event_tag
(
    event_id BIGINT UNSIGNED NOT NULL,
    tag      VARCHAR(255) NOT NULL,
    PRIMARY KEY (event_id, tag),
    FOREIGN KEY (event_id)
        REFERENCES event_journal (ordering)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS snapshot
(
    persistence_id        VARCHAR(255) NOT NULL,
    sequence_number       BIGINT       NOT NULL,
    created               BIGINT       NOT NULL,
    snapshot_ser_id       INTEGER      NOT NULL,
    snapshot_ser_manifest TEXT         NOT NULL,
    snapshot_payload      BLOB         NOT NULL,
    meta_ser_id           INTEGER,
    meta_ser_manifest     TEXT,
    meta_payload          BLOB,
    PRIMARY KEY (persistence_id, sequence_number)
);

CREATE TABLE IF NOT EXISTS durable_state
(
    global_offset         SERIAL,
    persistence_id        VARCHAR(255) NOT NULL,
    revision              BIGINT       NOT NULL,
    state_payload         BLOB         NOT NULL,
    state_serial_id       INTEGER      NOT NULL,
    state_serial_manifest VARCHAR(255),
    tag                   VARCHAR(255),
    state_timestamp       BIGINT       NOT NULL,
    PRIMARY KEY (persistence_id)
);
CREATE INDEX state_tag_idx on durable_state (tag);
CREATE INDEX state_global_offset_idx on durable_state (global_offset);

CREATE SEQUENCE IF NOT EXISTS durable_state_global_offset_seq
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    START 1
    CACHE 1;
