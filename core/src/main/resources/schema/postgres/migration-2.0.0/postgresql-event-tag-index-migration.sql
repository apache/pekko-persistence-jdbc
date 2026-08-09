-- SPDX-License-Identifier: Apache-2.0

-- see https://github.com/apache/pekko-persistence-jdbc/pull/593

-- Add a tag-leading index on event_tag for efficient eventsByTag queries.
-- The PK (persistence_id, sequence_number, tag) has tag as the third column,
-- so it cannot be used efficiently for tag-first lookups.
CREATE INDEX CONCURRENTLY IF NOT EXISTS event_tag_idx on public.event_tag(tag);
