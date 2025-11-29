-- SPDX-License-Identifier: Apache-2.0

-- see https://github.com/apache/pekko-persistence-jdbc/pull/382

-- Drop primary key constraint on event_journal to allow altering column types

DECLARE @pkName sysname;

SELECT @pkName = name
FROM sys.key_constraints
WHERE parent_object_id = OBJECT_ID('event_journal')
  AND type = 'PK';

IF @pkName IS NOT NULL
    EXEC('ALTER TABLE event_journal DROP CONSTRAINT ' + QUOTENAME(@pkName));

ALTER TABLE event_journal ALTER COLUMN
  "persistence_id" NVARCHAR(255);

ALTER TABLE event_journal
  ADD CONSTRAINT PK_event_journal PRIMARY KEY ("persistence_id", "sequence_number");

ALTER TABLE event_journal ALTER COLUMN
  "writer" NVARCHAR(255);

ALTER TABLE event_journal ALTER COLUMN
  "adapter_manifest" NVARCHAR(MAX);

ALTER TABLE event_journal ALTER COLUMN
  "event_ser_manifest" NVARCHAR(MAX);

ALTER TABLE event_journal ALTER COLUMN
  "meta_ser_manifest" NVARCHAR(MAX);

-- Drop primary key constraint on event_tag to allow altering column types

SELECT @pkName = name
FROM sys.key_constraints
WHERE parent_object_id = OBJECT_ID('event_tag')
  AND type = 'PK';

IF @pkName IS NOT NULL
    EXEC('ALTER TABLE "event_tag" DROP CONSTRAINT ' + QUOTENAME(@pkName));

ALTER TABLE "event_tag" ALTER COLUMN
  "tag" NVARCHAR(255);

ALTER TABLE "event_tag"
  ADD CONSTRAINT PK_event_tag PRIMARY KEY ("event_id", "tag");

-- Drop primary key constraint on snapshot to allow altering column types

SELECT @pkName = name
FROM sys.key_constraints
WHERE parent_object_id = OBJECT_ID('snapshot')
  AND type = 'PK';

IF @pkName IS NOT NULL
    EXEC('ALTER TABLE "snapshot" DROP CONSTRAINT ' + QUOTENAME(@pkName));

ALTER TABLE "snapshot" ALTER COLUMN
  "persistence_id" NVARCHAR(255);

ALTER TABLE "snapshot"
  ADD CONSTRAINT PK_snapshot PRIMARY KEY ("persistence_id", "sequence_number");

ALTER TABLE "snapshot" ALTER COLUMN
  "snapshot_ser_manifest" NVARCHAR(255);

ALTER TABLE "snapshot" ALTER COLUMN
  "meta_ser_manifest" NVARCHAR(255);

-- Drop primary key constraint on durable_state to allow altering column types

SELECT @pkName = name
FROM sys.key_constraints
WHERE parent_object_id = OBJECT_ID('durable_state')
  AND type = 'PK';

IF @pkName IS NOT NULL
    EXEC('ALTER TABLE durable_state DROP CONSTRAINT ' + QUOTENAME(@pkName));

ALTER TABLE durable_state ALTER COLUMN
  "persistence_id" NVARCHAR(255);

ALTER TABLE durable_state
  ADD CONSTRAINT PK_durable_state PRIMARY KEY ("persistence_id");

ALTER TABLE durable_state ALTER COLUMN
  "state_serial_manifest" NVARCHAR(MAX);

ALTER TABLE durable_state ALTER COLUMN
  "tag" NVARCHAR(255);
