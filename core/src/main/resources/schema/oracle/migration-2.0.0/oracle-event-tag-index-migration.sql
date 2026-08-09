-- SPDX-License-Identifier: Apache-2.0

-- Add a tag-leading index on event_tag for efficient eventsByTag queries.
-- The PK (persistence_id, sequence_number, tag) has tag as the third column,
-- so it cannot be used efficiently for tag-first lookups.
CREATE INDEX EVENT_TAG_IDX ON EVENT_TAG (TAG)
/

-- Add a global_offset index on durable_state for efficient changesByTag queries.
-- This index was already in place for other databases, but was missing for Oracle.
CREATE INDEX STATE_GLOBAL_OFFSET_IDX ON DURABLE_STATE (GLOBAL_OFFSET)
/
