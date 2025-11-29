-- SPDX-License-Identifier: Apache-2.0

-- see https://github.com/apache/pekko-persistence-jdbc/pull/365

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
