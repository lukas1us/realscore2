-- When a listing's detail was last fetched, whether or not the payload changed (TASK-31).
-- listing_snapshot.fetched_at only moves on a change, and last_seen_at moves on every run that collects the ID.
ALTER TABLE listing ADD COLUMN last_fetched_at TIMESTAMPTZ;

UPDATE listing l
SET last_fetched_at = (SELECT max(s.fetched_at) FROM listing_snapshot s WHERE s.listing_id = l.id);

ALTER TABLE listing ALTER COLUMN last_fetched_at SET NOT NULL;
