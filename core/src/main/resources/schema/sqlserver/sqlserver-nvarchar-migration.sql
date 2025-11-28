-- SPDX-License-Identifier: Apache-2.0

-- see https://github.com/apache/pekko-persistence-jdbc/pull/382

ALTER TABLE event_journal ALTER COLUMN
  "persistence_id" NVARCHAR(255),
  "writer" NVARCHAR(255),
  "adapter_manifest" NVARCHAR(MAX),
  "event_ser_manifest" NVARCHAR(MAX),
  "meta_ser_manifest" NVARCHAR(MAX);

ALTER TABLE "snapshot" ALTER COLUMN
  "tag" NVARCHAR(255);

ALTER TABLE "snapshot" ALTER COLUMN
  "persistence_id" NVARCHAR(255),
  "snapshot_ser_manifest" NVARCHAR(255),
  "meta_ser_manifest" NVARCHAR(255);

ALTER TABLE durable_state ALTER COLUMN
  "persistence_id" NVARCHAR(255),
  "state_serial_manifest" NVARCHAR(MAX),
  "tag" NVARCHAR(255);
