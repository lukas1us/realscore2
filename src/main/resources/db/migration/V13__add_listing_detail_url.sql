-- TASK-43: the public Sreality detail URL, derived by parsing from the raw detail payload. NULL when the payload
-- lacks a part the URL needs. Existing rows are filled by the next reparse
-- (POST /api/parsing/reparse?source=sreality): the URL cannot be built in SQL from the stored columns.
ALTER TABLE listing_detail
    ADD COLUMN url TEXT;
