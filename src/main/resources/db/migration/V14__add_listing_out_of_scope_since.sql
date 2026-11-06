-- TASK-47: a listing the search no longer returns (price raised above the price cap, switched to price on request)
-- but whose detail still answers 200 is online, yet outside the collected scope. It used to stay active like any
-- other listing and have its detail fetched on every run. out_of_scope_since is when the removal check first found
-- it so; NULL means in scope (or removed). It is cleared when a run's search returns the listing again.
-- No backfill: the next run's removal check flags such listings, since the search does not return them.
ALTER TABLE listing
    ADD COLUMN out_of_scope_since TIMESTAMPTZ;
