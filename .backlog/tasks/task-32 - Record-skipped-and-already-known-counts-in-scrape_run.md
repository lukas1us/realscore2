---
id: TASK-32
title: Record skipped and already-known counts in scrape_run
status: Done
assignee:
  - '@claude'
created_date: '2026-10-31T22:34:00Z'
updated_date: '2026-11-01T13:25:00Z'
labels:
  - scraping
dependencies:
  - TASK-18
ordinal: 32000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SrealityBulkCollector (TASK-16) returns BulkCollectionResult(found, alreadyKnown, fetched, skipped, failed), but scrape_run (V3 migration, TASK-17) only has found/new/updated/removed/error columns, so SrealityScrapeJob (TASK-18) stores found, new=fetched and errors=failed and the skipped (HTTP 403/404) and already-known counts appear only in the log. Add columns for them through a new Flyway migration (never edit V3), map them in ScrapeRun/ScrapeRunService, and have the job record them.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A new Flyway migration adds skipped and already-known count columns to scrape_run without editing existing migrations
- [x] #2 SrealityScrapeJob records the skipped and already-known counts of each run
- [x] #3 A Testcontainers-backed test asserts both counts are stored for a run that has skipped and already-known IDs
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. V7__add_scrape_run_skipped_and_already_known_counts.sql: ADD COLUMN skipped_count / already_known_count INTEGER NOT NULL DEFAULT 0 (existing rows get 0).
2. ScrapeRun: two new int fields mapped to those columns.
3. ScrapeRunService.updateCounts: replace the positional int list (would grow to 7 ints) with a public record ScrapeRunCounts(found, newCount, updated, removed, skipped, alreadyKnown, errors); update the one caller and ScrapeRunServiceTest.
4. SrealityScrapeJob passes result.skipped() and result.alreadyKnown().
5. SrealityScrapeJobTest: run with one pre-recorded (known, not due) ID and one new ID answering 404, via a new search fixture with fresh IDs (4701/4702) so it can't collide with other tests' IDs; assert skipped_count=1, already_known_count=1.
6. mvn verify; check README.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented as planned (user approved the ScrapeRunCounts record over extra positional ints). V7 migration adds skipped_count/already_known_count (NOT NULL DEFAULT 0); ScrapeRun maps them; ScrapeRunService.updateCounts(runId, ScrapeRunCounts); SrealityScrapeJob passes result.skipped()/alreadyKnown(). New fixture search_page_skipped_known.json (IDs 4701/4702) keeps the new SrealityScrapeJobTest case independent of other tests' IDs in the shared container.

AC #1: V7__add_scrape_run_skipped_and_already_known_counts.sql (V1-V6 untouched; applies from scratch in Testcontainers). AC #2: SrealityScrapeJob passes result.skipped()/alreadyKnown() via ScrapeRunCounts. AC #3: SrealityScrapeJobTest.runRecordsSkippedAndAlreadyKnownCounts (Testcontainers + WireMock) asserts both columns = 1; ScrapeRunServiceTest covers the mapping. README unaffected (no endpoint/config change). No follow-ups.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
scrape_run now stores each run's skipped (HTTP 403/404) and already-known counts, which previously appeared only in the log. New migration V7 adds skipped_count and already_known_count (INTEGER NOT NULL DEFAULT 0, so existing runs read 0); ScrapeRun maps them; SrealityScrapeJob records BulkCollectionResult.skipped()/alreadyKnown(). ScrapeRunService.updateCounts now takes a public ScrapeRunCounts record instead of a growing list of positional ints (user-approved), so the counts can't be silently swapped. Tested with a Testcontainers + WireMock job run that has one known and one 404 ID (new fixture search_page_skipped_known.json, fresh IDs to stay isolated). mvn verify passes (100 tests).
<!-- SECTION:FINAL_SUMMARY:END -->
