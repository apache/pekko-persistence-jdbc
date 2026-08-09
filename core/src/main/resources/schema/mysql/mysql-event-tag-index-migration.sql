-- Add a tag-leading index on event_tag for efficient eventsByTag queries.
-- The PK (persistence_id, sequence_number, tag) has tag as the third column,
-- so it cannot be used efficiently for tag-first lookups.
CREATE INDEX event_tag_idx on event_tag (tag);
