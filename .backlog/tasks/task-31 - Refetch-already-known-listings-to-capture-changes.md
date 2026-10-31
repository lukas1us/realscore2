---
id: TASK-31
title: Refetch already-known listings to capture changes
status: Done
assignee:
  - '@claude'
created_date: '2026-10-31T21:32:00Z'
updated_date: '2026-10-31T23:25:00Z'
labels:
  - scraping
  - listing
dependencies:
  - TASK-16
ordinal: 31000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SrealityBulkCollector (TASK-16) fetches details only for collected IDs that have no listing yet; IDs that are already known are skipped entirely. As a result, price or other payload changes on known listings are never captured, and their last_seen_at is not updated by the bulk run, even though ListingSnapshotService.recordFetch already writes a new snapshot when the payload hash changes. Decide on and implement a refetch strategy for known listings (e.g. refetch every known collected ID each run, or only those not fetched within a configurable interval), keeping request volume within the shared Sreality rate-limit policy (TASK-15). Coordinate with TASK-19, which uses per-run 'seen' information for removal detection.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Known listings that appear in a bulk run's collected IDs are refetched according to a documented, configurable strategy
- [x] #2 A refetched listing whose payload changed gets a new snapshot; an unchanged one only has last_seen_at updated
- [x] #3 A Testcontainers-backed test covers a known listing being refetched with a changed and with an unchanged payload
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. V5__add_listing_last_fetched_at.sql: add listing.last_fetched_at TIMESTAMPTZ, backfill from max(listing_snapshot.fetched_at), set NOT NULL. Listing.lastFetchedAt.
2. listing: recordFetch sets last_fetched_at on every fetch and returns new public enum RecordedFetch {CREATED, CHANGED, UNCHANGED}; new findExternalIdsFetchedBefore(source, ids, cutoff) (batched).
3. scraping: realscore.scrape.refetch-after (Duration, default 7d, 0 = every run) in ScrapeJobProperties; SrealityEstateFetcher.fetch -> Optional<RecordedFetch>; SrealityBulkCollector.collect(priceCap, refetchAfter) fetches new IDs plus known IDs last fetched before now - refetchAfter; BulkCollectionResult adds refetched and changed; SrealityScrapeJob stores updated = changed.
4. Tests: SrealityScrapeJobTest refetch scenario (changed -> 2 snapshots + updated_count 1; unchanged -> 1 snapshot, last_fetched_at moved; recently fetched known listing not requested); ListingSnapshotServiceTest for RecordedFetch and findExternalIdsFetchedBefore; adapt fetcher/bulk tests.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Note from TASK-19: SrealityScrapeJob now sets last_seen_at (and clears removed_at) for every listing a run collects, known or not, via ListingSnapshotService.recordPresence. So the 'unchanged one only has last_seen_at updated' part of AC #2 is already true for seen listings; this task still needs the refetch itself and the scrape_run updated_count.

Implemented as planned. V5__add_listing_last_fetched_at.sql adds listing.last_fetched_at (backfilled from max(listing_snapshot.fetched_at), then NOT NULL); needed because snapshot fetched_at only moves on change and last_seen_at moves on every run (TASK-19). listing: recordFetch sets last_fetched_at on every fetch and returns new public enum RecordedFetch {CREATED, CHANGED, UNCHANGED}; findExternalIdsFetchedBefore(source, ids, cutoff), batched like findKnownExternalIds. scraping: strategy = realscore.scrape.refetch-after (Duration, default 7d via SCRAPE_REFETCH_AFTER, 0 = every run), documented in ScrapeJobProperties Javadoc and application.properties; SrealityBulkCollector.collect(priceCap, refetchAfter) fetches new IDs then known IDs last fetched before now - refetchAfter in one loop; BulkCollectionResult adds refetched and changed; SrealityEstateFetcher.fetch returns Optional<RecordedFetch>; SrealityScrapeJob stores updated_count = changed. Tests: SrealityScrapeJobTest refetch scenario (4501 changed -> 2 snapshots, 4502 unchanged -> 1 snapshot with last_fetched_at moved, 4503 recently fetched -> not requested, updated_count 1, new_count 0), ListingSnapshotServiceTest +3 (RecordedFetch values, last_fetched_at moves on unchanged fetch, fetched-before cutoff); fetcher/bulk tests adapted. Migration applies from scratch in every Testcontainers context; the backfill UPDATE itself is not exercised against pre-existing rows. mvn verify: 61 tests, BUILD SUCCESS.

Finalized via /finish-task: mvn verify green (61 tests). AC evidence: #1 realscore.scrape.refetch-after (ScrapeJobProperties Javadoc, application.properties) + SrealityScrapeJobTest.knownListingsLastFetchedBeforeTheRefetchIntervalAreFetchedAgain (4503 not due, not requested) + ListingSnapshotServiceTest.findExternalIdsFetchedBeforeReturnsOnlyListingsLastFetchedBeforeTheCutoff; #2 same job test (4501 -> 2 snapshots, 4502 -> 1 snapshot and last_fetched_at moved) + recordFetchReportsWhetherTheListingWasCreatedChangedOrUnchanged; #3 the job test on Testcontainers Postgres. No new follow-up tasks.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Known listings are now refetched so their changes are captured. Strategy: a listing that a run collects is fetched again once its last fetch is older than realscore.scrape.refetch-after (default 7 days, SCRAPE_REFETCH_AFTER; 0 = every run). This needed a new column, listing.last_fetched_at (migration V5, backfilled from the latest snapshot), since neither snapshot fetched_at nor last_seen_at says when a listing was last fetched. recordFetch now reports CREATED / CHANGED / UNCHANGED; a changed payload writes a new snapshot, an unchanged one only moves the timestamps, and scrape_run.updated_count now holds the number of changed listings. Tests on Testcontainers + WireMock cover a changed refetch, an unchanged refetch and a not-yet-due listing that is left alone. mvn verify green, 61 tests. Risk: the V5 backfill is only exercised on an empty database.
<!-- SECTION:FINAL_SUMMARY:END -->
