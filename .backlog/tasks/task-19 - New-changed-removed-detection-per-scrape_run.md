---
id: TASK-19
title: New/changed/removed detection per scrape_run
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-10-31T23:13:00Z'
labels:
  - listing
milestone: m-4
dependencies:
  - TASK-18
ordinal: 19000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Price/payload changes are already captured as new snapshots by M2's persistence logic; what's still missing is marking listings that stopped appearing in a run as removed, and summarising what changed in that run's scrape_run record. Scoped narrowly to removal detection and the run summary -- not a re-implementation of snapshot-level change detection.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 After a scheduled run completes, any previously-active listing not seen in that run has removed_at set
- [x] #2 A listing that reappears in a later run after being marked removed has removed_at cleared
- [x] #3 The owning scrape_run record is updated with counts of new, updated, and removed listings for that run
- [x] #4 A Testcontainers-backed test covers a listing being removed in one run and reappearing in a subsequent run
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. listing: ListingSnapshotService.recordPresence(source, seenExternalIds, runStartedAt) -> PresenceResult(reappeared, removed), one transaction: clear removed_at for seen listings that had it (counted), set last_seen_at=now for seen (batches of 1000), set removed_at=now where source matches, removed_at is null and last_seen_at < runStartedAt. Three @Modifying JPQL updates in ListingRepository.
2. scraping: BulkCollectionResult carries foundIds (found() = size; toString without the IDs). SrealityScrapeJob captures the start time; after a successful collection calls recordPresence and stores removed; skips removal with a warning when 0 IDs were found; failed runs never mark removals. updated stays 0 until TASK-31.
3. Tests: SrealityScrapeJobTest three-run scenario (4301/4302 seen, 4302 removed, 4302 reappears without refetch), failed run removes nothing, empty run removes nothing; ListingSnapshotServiceTest recordPresence case on an isolated source.
4. Note on TASK-31 that last_seen_at for seen known listings is now updated here.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented as planned. 'Seen' = in the run's collected IDs (known listings are not refetched, so last_seen_at alone can't tell). listing: ListingSnapshotService.recordPresence(source, seenIds, runStartedAt) -> PresenceResult(reappeared, removed), one transaction, three @Modifying JPQL updates in ListingRepository (clear removed_at for seen, last_seen_at=now for seen, both in batches of 1000; then removed_at=now where removed_at is null and last_seen_at < runStartedAt, one statement). Batch constant renamed ID_BATCH_SIZE, shared with findKnownExternalIds. scraping: BulkCollectionResult now carries foundIds (found() = size, toString omits the IDs); SrealityScrapeJob calls recordPresence after a successful collection and stores removed in scrape_run; a run that found 0 IDs skips removal with a warning (user-approved guard); failed runs never mark removals. AC #3: new and removed are recorded; updated stays 0 by user decision until TASK-31 refetches known listings. Known semantic (accepted): a listing whose price rises above the cap is marked removed. Tests: SrealityScrapeJobTest +3 (three-run remove/reappear scenario incl. removed_count and no refetch of 4302; failed run and empty run remove nothing), ListingSnapshotServiceTest +1 (isolated source, other source untouched). mvn verify: 57 tests, BUILD SUCCESS.

Finalized via /finish-task: mvn verify green (57 tests). AC evidence: #1 SrealityScrapeJobTest.listingMissingFromARunIsMarkedRemovedAndClearedWhenItReappears (run 2) and ListingSnapshotServiceTest.recordPresenceMarksUnseenListingsRemovedAndClearsReappearedOnes; #2 same tests (run 3 / second call); #3 scrape_run new_count and removed_count asserted in SrealityScrapeJobTest (updated_count 0 until TASK-31, user decision); #4 the three-run SrealityScrapeJobTest scenario on Testcontainers Postgres. No new follow-ups beyond TASK-31/TASK-32.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Listings that stop appearing are now marked removed, and cleared again when they come back. After a successful scrape, SrealityScrapeJob passes the run's collected IDs to the new ListingSnapshotService.recordPresence, which in one transaction clears removed_at and sets last_seen_at for every seen listing, then sets removed_at on every still-active listing not seen since the run started. The run's removed count goes into scrape_run alongside new and errors; updated stays 0 until TASK-31. Safeguards: failed runs and runs that found no IDs at all mark nothing removed. Tests: a three-run Testcontainers + WireMock scenario (seen, removed, reappeared without refetch), both safeguards, and a listing-level test. mvn verify green, 57 tests. Caveat: a listing priced above the cap counts as removed.
<!-- SECTION:FINAL_SUMMARY:END -->
