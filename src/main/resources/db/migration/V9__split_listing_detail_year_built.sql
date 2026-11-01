-- TASK-41: year_built held acceptance_year with a fallback to reconstruction_year, so a reconstructed
-- old flat looked like a new build. Each year now has its own column. They are filled by the next reparse
-- (POST /api/parsing/reparse?source=sreality): year_built cannot be split here, it does not say which field it came from.
ALTER TABLE listing_detail
    ADD COLUMN acceptance_year INTEGER,
    ADD COLUMN reconstruction_year INTEGER;

ALTER TABLE listing_detail DROP COLUMN year_built;
