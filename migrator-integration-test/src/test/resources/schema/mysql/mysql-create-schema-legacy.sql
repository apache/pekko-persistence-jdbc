CREATE TABLE IF NOT EXISTS journal (
  ordering SERIAL,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  deleted BOOLEAN DEFAULT FALSE NOT NULL,
  tags VARCHAR(255) DEFAULT NULL,
  message BLOB NOT NULL,
  PRIMARY KEY(persistence_id, sequence_number)
);
CREATE UNIQUE INDEX journal_ordering_idx ON journal(ordering);

CREATE TABLE IF NOT EXISTS legacy_snapshot (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  created BIGINT NOT NULL,
  snapshot BLOB NOT NULL,
  PRIMARY KEY (persistence_id, sequence_number)
);

CREATE TABLE IF NOT EXISTS event_tag
(
    event_id BIGINT UNSIGNED,
    persistence_id     VARCHAR(255),
    sequence_number    BIGINT,
    tag      VARCHAR(255) NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number, tag),
    FOREIGN KEY (persistence_id, sequence_number)
    REFERENCES event_journal (persistence_id, sequence_number)
    ON DELETE CASCADE
    );
