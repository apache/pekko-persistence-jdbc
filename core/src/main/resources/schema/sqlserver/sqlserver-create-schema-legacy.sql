/*
Pekko Persistence JDBC versions up to 1.3.0 used VARCHAR instead of NVARCHAR for string fields.
This schema change is not applied automatically for existing databases so it is recommended
that users modify their database schema themselves to use NVARCHAR to avoid potential issues with
character encoding, especially when dealing with non-ASCII characters.
If you stick with VARCHAR fields, it is highly recommended to not have the SQL Server JDBC client send
strings as Unicode, by appending ;sendStringParametersAsUnicode=false to the JDBC connection string.
*/

IF  NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'"journal"') AND type in (N'U'))
begin
CREATE TABLE journal (
  "ordering" BIGINT IDENTITY(1,1) NOT NULL,
  "deleted" BIT DEFAULT 0 NOT NULL,
  "persistence_id" NVARCHAR(255) NOT NULL,
  "sequence_number" NUMERIC(10,0) NOT NULL,
  "tags" NVARCHAR(255) NULL DEFAULT NULL,
  "message" VARBINARY(max) NOT NULL,
  PRIMARY KEY ("persistence_id", "sequence_number")
)
CREATE UNIQUE INDEX journal_ordering_idx ON journal (ordering)
end;


IF  NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'"snapshot"') AND type in (N'U'))
CREATE TABLE snapshot (
  "persistence_id" NVARCHAR(255) NOT NULL,
  "sequence_number" NUMERIC(10,0) NOT NULL,
  "created" NUMERIC NOT NULL,
  "snapshot" VARBINARY(max) NOT NULL,
  PRIMARY KEY ("persistence_id", "sequence_number")
);
end;
