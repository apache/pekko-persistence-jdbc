-- SPDX-License-Identifier: Apache-2.0

-- see https://github.com/apache/pekko-persistence-jdbc/pull/382

ALTER TABLE event_journal ALTER COLUMN
  "persistence_id" NVARCHAR(255);

ALTER TABLE event_journal ALTER COLUMN
  "writer" NVARCHAR(255);

ALTER TABLE event_journal ALTER COLUMN
  "adapter_manifest" NVARCHAR(MAX);

ALTER TABLE event_journal ALTER COLUMN
  "event_ser_manifest" NVARCHAR(MAX);

ALTER TABLE event_journal ALTER COLUMN
  "meta_ser_manifest" NVARCHAR(MAX);

ALTER TABLE "snapshot" ALTER COLUMN
  "tag" NVARCHAR(255);

ALTER TABLE "snapshot" ALTER COLUMN
  "persistence_id" NVARCHAR(255);

ALTER TABLE "snapshot" ALTER COLUMN
  "snapshot_ser_manifest" NVARCHAR(255);

ALTER TABLE "snapshot" ALTER COLUMN
  "meta_ser_manifest" NVARCHAR(255);

ALTER TABLE durable_state ALTER COLUMN
  "persistence_id" NVARCHAR(255);

ALTER TABLE durable_state ALTER COLUMN
  "state_serial_manifest" NVARCHAR(MAX);

ALTER TABLE durable_state ALTER COLUMN
  "tag" NVARCHAR(255);
