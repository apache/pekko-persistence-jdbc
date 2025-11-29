-- SPDX-License-Identifier: Apache-2.0

-- see https://github.com/apache/pekko-persistence-jdbc/pull/323

ALTER TABLE EVENT_JOURNAL MODIFY DELETED NUMBER(1) DEFAULT 0 NOT NULL check (DELETED in (0, 1))
/
