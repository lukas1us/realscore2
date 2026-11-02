-- TASK-39: a price change used to be seen only when the detail was refetched after refetch-after (7 days by default),
-- so price drops were late and dated wrong. The scrape now keeps the price each listing had in the search results
-- and refetches the detail in the same run when it changes. The value is Sreality's raw price_summary_czk from the
-- search result: 0 means price on request, NULL means no search price has been recorded yet (nothing to compare).
ALTER TABLE listing
    ADD COLUMN search_price_czk NUMERIC;
