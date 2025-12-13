-- SPDX-License-Identifier: Apache-2.0

-- see https://github.com/apache/pekko-persistence-jdbc/pull/382

-- Drop primary key constraint on event_journal to allow altering column types

-- Script is provided as an example only and has only been partially tested.
-- Please review and test thoroughly before using in production and
-- ideally, in a test environment first.
-- Always back up your database before running migration scripts.

CREATE PROCEDURE DropPrimaryKey
    @TableName NVARCHAR(255)
AS
BEGIN
  SET NOCOUNT ON;
  DECLARE @PKName NVARCHAR(1024);

  SELECT @PKName = name
  FROM sys.key_constraints
  WHERE parent_object_id = OBJECT_ID(@TableName)
    AND type = 'PK';

  IF @PKName IS NOT NULL
    EXEC('ALTER TABLE ' + @TableName + ' DROP CONSTRAINT ' + @PKName);
END;

EXEC DropPrimaryKey 'event_journal';

ALTER TABLE event_journal ALTER COLUMN
  "persistence_id" NVARCHAR(255) NOT NULL;

ALTER TABLE event_journal
  ADD CONSTRAINT PK_event_journal PRIMARY KEY ("persistence_id", "sequence_number");

ALTER TABLE event_journal ALTER COLUMN
  "writer" NVARCHAR(255) NOT NULL;

ALTER TABLE event_journal ALTER COLUMN
  "adapter_manifest" NVARCHAR(MAX) NOT NULL;

ALTER TABLE event_journal ALTER COLUMN
  "event_ser_manifest" NVARCHAR(MAX) NOT NULL;

ALTER TABLE event_journal ALTER COLUMN
  "meta_ser_manifest" NVARCHAR(MAX);

-- Drop primary key constraint on event_tag to allow altering column types

EXEC DropPrimaryKey 'event_tag';

ALTER TABLE "event_tag" ALTER COLUMN
  "tag" NVARCHAR(255) NOT NULL;

ALTER TABLE "event_tag"
  ADD CONSTRAINT PK_event_tag PRIMARY KEY ("event_id", "tag");

-- Drop primary key constraint on snapshot to allow altering column types

EXEC DropPrimaryKey 'snapshot';

ALTER TABLE "snapshot" ALTER COLUMN
  "persistence_id" NVARCHAR(255) NOT NULL;

ALTER TABLE "snapshot"
  ADD CONSTRAINT PK_snapshot PRIMARY KEY ("persistence_id", "sequence_number");

ALTER TABLE "snapshot" ALTER COLUMN
  "snapshot_ser_manifest" NVARCHAR(255) NOT NULL;

ALTER TABLE "snapshot" ALTER COLUMN
  "meta_ser_manifest" NVARCHAR(255);

-- Drop primary key constraint on durable_state to allow altering column types

EXEC DropPrimaryKey 'durable_state';

ALTER TABLE durable_state ALTER COLUMN
  "persistence_id" NVARCHAR(255) NOT NULL;

ALTER TABLE durable_state
  ADD CONSTRAINT PK_durable_state PRIMARY KEY ("persistence_id");

ALTER TABLE durable_state ALTER COLUMN
  "state_serial_manifest" NVARCHAR(MAX);

ALTER TABLE durable_state ALTER COLUMN
  "tag" NVARCHAR(255);

-- Drop the procedure as it's no longer needed

DROP PROCEDURE DropPrimaryKey;
