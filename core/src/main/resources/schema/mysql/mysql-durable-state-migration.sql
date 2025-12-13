-- SPDX-License-Identifier: Apache-2.0

-- see https://github.com/apache/pekko-persistence-jdbc/pull/366

-- Script is provided as an example only and has only been partially tested.
-- Please review and test thoroughly before using in production and
-- ideally, in a test environment first.
-- Always back up your database before running migration scripts.

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

CREATE TABLE IF NOT EXISTS durable_state_global_offset
(
    singleton      TINYINT NOT NULL,
    current_offset BIGINT UNSIGNED NOT NULL UNIQUE,
    PRIMARY KEY (singleton)
);
INSERT INTO durable_state_global_offset (singleton, current_offset) VALUES (0, 0);
