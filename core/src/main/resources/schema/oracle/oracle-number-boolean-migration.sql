-- SPDX-License-Identifier: Apache-2.0

-- see https://github.com/apache/pekko-persistence-jdbc/pull/323

-- Script is provided as an example only and only been partially tested.
-- Please review and test thoroughly before using in production and
-- ideally, in a test environment first.
-- Always back up your database before running migration scripts.

ALTER TABLE EVENT_JOURNAL MODIFY DELETED NUMBER(1) DEFAULT 0 NOT NULL check (DELETED in (0, 1))
/
