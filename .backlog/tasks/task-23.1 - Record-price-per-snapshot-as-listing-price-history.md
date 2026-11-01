---
id: TASK-23.1
title: Record price per snapshot as listing price history
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T14:16:00Z'
updated_date: '2026-11-01T14:29:00Z'
labels:
  - analysis
milestone: m-6
dependencies: []
parent_task_id: TASK-23
type: feature
ordinal: 39000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
listing_detail keeps only the latest parse of a listing, so earlier asking prices exist only inside older listing_snapshot payloads. Price changes are the main signal wanted from the stored history (later used for scoring, TASK-24), so they should be queryable without re-parsing JSONB on every read. Payloads also change for reasons other than price, so not every snapshot is a price change.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Each parsed snapshot yields a price history point (listing, observed time = snapshot fetched_at, price in CZK); a snapshot whose price equals the previous point does not add a new point
- [x] #2 New and changed snapshots are added to the history automatically after a scrape run, like listing_detail
- [x] #3 POST /api/parsing/reparse rebuilds the history of the selected listings from all their stored snapshots, and running it twice gives the same result
- [x] #4 Snapshots without a parseable price are skipped without failing the parse
- [x] #5 Tests cover new, unchanged-price, changed-price and reparse cases
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Migration V8 (via new-migration skill): table listing_price_point (listing_id FK, observed_at = snapshot fetched_at, price_czk NOT NULL), UNIQUE(listing_id, observed_at).
2. listing: ListingSnapshotService.findSnapshots(source, externalId) returning every snapshot of a listing ordered by fetched_at (existing ListingSnapshotView).
3. parsing/internal: ListingPricePoint entity + repository.
4. ListingDetailsParsingService.parse also rebuilds the listing's price history from all its snapshots: extract price from each with the existing extractor, skip null prices, collapse consecutive equal prices, and replace the stored points only if they differ. One code path covers the post-scrape event and POST /api/parsing/reparse, and is idempotent.
5. ReparseResult counts keep describing listing_detail only.
6. Tests: parsing service test for new / same price / changed price / missing price / reparse twice.
7. README: mention price history under Parsing, and an ops note to run reparse once to backfill existing listings.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Price history is rebuilt from all snapshots on every parse (one code path for the post-scrape listener and reparse), replacing stored points only when they differ. The delete uses a JPQL bulk delete so it runs before the replacement inserts (Hibernate queues inserts before entity deletes, which would hit the unique constraint). parse() now reads all snapshots via the new ListingSnapshotService.findSnapshots and uses the last one for listing_detail. mvn verify: 106 tests, BUILD SUCCESS.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Adds listing_price_point (V8): one row per asking-price change per listing, observed_at = fetched_at of the first snapshot with that price; snapshots without a price are skipped. ListingDetailsParsingService.parse rebuilds it from all snapshots (ListingPriceHistoryUpdater), so it updates after each scrape and POST /api/parsing/reparse backfills/rebuilds it idempotently. Reparse counts still describe listing_detail only. New ListingSnapshotService.findSnapshots. Tests: ListingPriceHistoryTest (new, same price, changes, no price, reparse twice) and findSnapshots. README updated. Follow-up: existing deployments must run reparse once to backfill history.
<!-- SECTION:FINAL_SUMMARY:END -->
