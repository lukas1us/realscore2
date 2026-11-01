---
id: TASK-21
title: Re-run parsing over existing snapshots
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-11-01T12:14:00Z'
labels:
  - parsing
milestone: m-5
dependencies:
  - TASK-20
ordinal: 21000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Normalisation logic will keep changing as new edge cases are found (as happened repeatedly in the old app's fix: commits); this task makes it possible to re-derive listing details for all already-stored snapshots whenever the parser changes, without any new scraping.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A command/job re-parses every stored snapshot (or a specified subset) and overwrites the corresponding listing-details rows
- [x] #2 Running it twice in a row with no parser changes in between produces identical listing-details rows
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. listing: add public ListingSnapshotService.findExternalIds(source) returning every listing external ID for a source (incl. removed).
2. ListingDetail.applyFields returns boolean changed; compares each field (BigDecimal via compareTo) and only moves parsed_at when something changed.
3. ListingDetailsParsingService.parse returns ParseOutcome (CREATED/UPDATED/UNCHANGED) and saves only new/changed rows - makes a no-change re-run produce identical rows incl. parsed_at (AC #2).
4. New public ListingDetailsReparseService: reparse(source) and reparse(source, externalIds); each listing parsed in its own transaction from its latest snapshot; per-listing failures logged+counted, not aborting; returns ReparseResult(created, updated, unchanged, failed).
5. Trigger: POST /api/parsing/reparse?source=sreality[&externalId=...] controller returning ReparseResult as JSON (user choice).
6. Tests (Testcontainers): full reparse creates rows; second run all UNCHANGED with identical rows; subset only touches given IDs; malformed payload counted as failed while others parse; controller test; unit test for applyFields change detection.
7. mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Made a no-change re-run produce identical rows: ListingDetail.applyFields now returns whether anything changed and only moves parsed_at when it did (decimals compared by value, so NUMERIC scale round-trips are not changes); parse() returns ParseOutcome and skips the save when UNCHANGED. ListingDetailsReparseService loops over listing external IDs (new ListingSnapshotService.findExternalIds) calling parse() per listing, each in its own transaction, catching and counting per-listing failures. Trigger: POST /api/parsing/reparse?source=...[&externalId=...] (user's choice over a startup runner). Runs synchronously - fine at current scale; very large reparses may need an async/batched variant later. Tests use a distinct source per test to stay isolated in the shared Postgres container.

AC #1 proven by ListingDetailsReparseServiceTest (reparsesEveryStoredListingOfTheSource, reparsesOnlyTheGivenSubset, aNewerSnapshotOverwritesTheRow, endpointReparsesAllOrTheGivenIds). AC #2 proven by ListingDetailsReparseServiceTest.secondRunWithoutParserChangesLeavesRowsIdentical (recursive row comparison incl. parsed_at) and ListingDetailTest. Follow-up: newly scraped snapshots are still never parsed automatically - recorded as a separate task.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added re-parsing of stored snapshots without new scraping: POST /api/parsing/reparse?source=...[&externalId=...] -> ListingDetailsReparseService (all listings of a source incl. removed, or a subset), parsing each listing's latest snapshot in its own transaction and counting per-listing failures instead of aborting; returns ReparseResult(created, updated, unchanged, failed). To make a no-change re-run produce identical rows, ListingDetail.applyFields now only writes and moves parsed_at when a field actually changed (decimals compared by value), and parse() returns ParseOutcome. Listing module gained public ListingSnapshotService.findExternalIds(source). No new dependencies or migrations. mvn verify passes (73 tests incl. ModularityTests). Risks: endpoint is synchronous and unauthenticated; very large re-parses may exceed HTTP timeouts.
<!-- SECTION:FINAL_SUMMARY:END -->
