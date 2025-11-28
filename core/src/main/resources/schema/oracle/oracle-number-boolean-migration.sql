-- SPDX-License-Identifier: Apache-2.0

-- see https://github.com/apache/pekko-persistence-jdbc/pull/323

ALTER TABLE EVENT_JOURNAL MODIFY "deleted" NUMBER(1)
/
