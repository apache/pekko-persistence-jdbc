-- SPDX-License-Identifier: Apache-2.0

-- see https://github.com/apache/pekko-persistence-jdbc/pull/323

-- Script is provided as an example only and has only been partially tested.
-- Please review and test thoroughly before using in production and
-- ideally, in a test environment first.
-- Always back up your database before running migration scripts.

ALTER TABLE DURABLE_STATE MODIFY TAG VARCHAR(255) NULL
/
