---
id: TASK-33
title: Parse new and changed snapshots after each scrape run
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T12:14:00Z'
updated_date: '2026-11-01T12:42:00Z'
labels:
  - parsing
milestone: m-5
dependencies:
  - TASK-21
ordinal: 33000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Nothing calls ListingDetailsParsingService automatically yet: snapshots written by the Sreality scrape job only get listing_detail rows when someone triggers POST /api/parsing/reparse (TASK-21). New and changed listings from each scrape run should be parsed without a manual step, keeping the dependency direction scraping -> listing <- parsing (scraping must not call parsing directly; e.g. an event published by listing, or a parsing-side schedule).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Listings created or changed by a scrape run get an up-to-date listing_detail row without a manual trigger
- [x] #2 Scraping does not depend on the parsing module (ModularityTests passes)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. listing: add public record ListingSnapshotRecorded(source, externalId); ListingSnapshotService.recordFetch publishes it via ApplicationEventPublisher when the result is CREATED or CHANGED (not UNCHANGED).
2. parsing.internal: SnapshotRecordedListener with @TransactionalEventListener(AFTER_COMMIT) + @Transactional(NOT_SUPPORTED), calling ListingDetailsParsingService.parse in its own fresh transaction; catches and logs failures so a parse problem never fails the scrape.
3. Synchronous (same thread as scraping) - parsing is ms vs. the 500ms request pacing; no new dependency (no Modulith event registry).
4. Tests (Testcontainers, parsing module): recordFetch of a new listing creates its listing_detail; a changed payload updates it; a payload that can't be stored still records the snapshot and returns CREATED without a detail row. listing test: event published for CREATED/CHANGED only.
5. ModularityTests + mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented as planned: listing publishes ListingSnapshotRecorded from recordFetch when a snapshot is written (CREATED/CHANGED); parsing.internal.SnapshotRecordedListener parses it after commit (@TransactionalEventListener AFTER_COMMIT + @Transactional(NOT_SUPPORTED) so parse() gets its own fresh transaction - joining the finished one would silently drop the writes), swallowing+logging failures so scraping is never affected. Synchronous, no new dependency. TASK-21's ListingDetailsReparseServiceTest had to change: recordFetch now parses immediately, so its setup now simulates pre-auto-parse listings (detail row deleted) and older-parser output (a field overwritten). One-off after deploying: POST /api/parsing/reparse?source=sreality to cover listings stored before this change.

AC #1 proven by SnapshotRecordedListenerTest (aNewListingGetsItsDetailRow, aChangedListingGetsItsDetailRowUpdated) - driven through ListingSnapshotService.recordFetch, the call SrealityEstateFetcher makes per fetched listing in every scrape run - plus ListingSnapshotServiceTest.announcesOnlyFetchesThatWroteASnapshot. AC #2 proven by ModularityTests (scraping imports nothing from parsing; parsing only uses listing's public API). No follow-ups.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
New and changed listings are now parsed automatically during scraping. listing publishes a public ListingSnapshotRecorded(source, externalId) event from recordFetch whenever a snapshot is written (CREATED/CHANGED, not UNCHANGED). parsing.internal.SnapshotRecordedListener handles it after the snapshot transaction commits, running parse() in its own fresh transaction (NOT_SUPPORTED suspends the finished one, which would otherwise silently swallow the writes) and logging rather than propagating failures, so scraping is never affected and a failed listing can be fixed with the TASK-21 reparse. Synchronous on the scraping thread; plain Spring events, no new dependency. TASK-21's reparse tests were adapted because recordFetch now parses immediately. mvn verify passes (77 tests incl. ModularityTests). After deploying, run POST /api/parsing/reparse?source=sreality once to cover listings stored before this change.
<!-- SECTION:FINAL_SUMMARY:END -->
