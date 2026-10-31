---
id: TASK-16
title: Bulk fetch and persist using the ID collector
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-10-31T22:13:00Z'
labels:
  - scraping
  - listing
milestone: m-3
dependencies:
  - TASK-14
  - TASK-15
  - TASK-12
ordinal: 16000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The end-to-end bulk path: run the ID collector for a price-capped sale-apartment search, fetch each newly-seen ID's detail through the rate-limited client, and persist each via the same listing+snapshot pipeline built for a single listing in M2. This is the piece M4's scheduled job will later wrap.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Running the pipeline for a given price cap persists a listing+snapshot for every collected ID not already known
- [x] #2 An ID that fails with a non-retriable error (403/404) is skipped without aborting the rest of the run
- [x] #3 An integration test using a handful of fixture IDs, including one 404 and one 429-then-success, verifies the expected listings are persisted and the failures are handled as specified
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. listing: ListingSnapshotService.findKnownExternalIds(source, externalIds) -> Set<String>, backed by a JPQL select on ListingRepository (one query).
2. scraping: SrealityBulkCollector.collect(priceCap): SrealityIdCollector IDs -> drop known -> SrealityEstateFetcher.fetch each; false (403/404) = skipped; SrealityApiException / RestClientException / IllegalStateException per ID = failed, warn and continue; ID collection failure aborts. Returns BulkCollectionResult(found, alreadyKnown, fetched, skipped, failed). No scrape_run handling (TASK-18).
3. Tests: SrealityBulkCollectorTest (SpringBootTest + Testcontainers + WireMock, 0ms delay, 1ms backoff): 4001 ok, 4002 404, 4003 429->200, 4004 pre-known; second test with a persistent 500 counted as failed. ListingSnapshotServiceTest case for findKnownExternalIds. Fixture search_page_bulk.json.
Follow-up: refetching known listings tracked as a new task (user approved).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented as planned. listing: ListingSnapshotService.findKnownExternalIds(source, ids) (read-only tx, JPQL select of externalId, batches of 1000 to stay under Postgres' bind-parameter limit). scraping: SrealityBulkCollector.collect(priceCap) -> BulkCollectionResult(found, alreadyKnown, fetched, skipped, failed); per-ID SrealityApiException / RestClientException / IllegalStateException counted as failed and logged, ID collection failure propagates. No scrape_run handling (TASK-18). Follow-up TASK-31 created (refetch known listings), user-approved. Tests: SrealityBulkCollectorTest (2, SpringBootTest + Testcontainers + WireMock; fixtures search_page_bulk.json, search_page_bulk_with_failure.json), ListingSnapshotServiceTest +2 (source filtering; 2501 IDs). The batch test proves correctness across sizes, not that batching happens. mvn verify: 50 tests, BUILD SUCCESS.

Finalized via /finish-task: mvn verify green (50 tests). AC evidence: #1 SrealityBulkCollectorTest.persistsNewEstatesSkipsGoneOnesRetriesThrottledOnesAndLeavesKnownOnesAlone (4001/4003 persisted, 4004 known and not requested) and ListingSnapshotServiceTest.findKnownExternalIds*; #2 same test (4002 404 skipped, run continues) plus persistentFailureForOneEstateIsCountedAndTheRestIsStillFetched; #3 SrealityBulkCollectorTest (4 fixture IDs incl. 404 and 429-then-success). Follow-up: TASK-31.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added the end-to-end bulk path: SrealityBulkCollector.collect(priceCap) collects sale-apartment IDs with SrealityIdCollector, drops those that already have a listing (new ListingSnapshotService.findKnownExternalIds, one batched query), and fetches and records each new one via SrealityEstateFetcher, so the TASK-15 retry/pacing policy applies. 403/404 count as skipped; any other per-estate failure is counted as failed and logged, and the run continues; a failure while collecting IDs aborts it. Returns BulkCollectionResult counts for TASK-18 to store in scrape_run. Integration test against Postgres and WireMock: new ID persisted, 404 skipped, 429-then-200 persisted after a retry, known ID not refetched, persistent 500 counted as failed while the rest is fetched. mvn verify green, 50 tests. Follow-up: TASK-31, refetching known listings so their changes are captured.
<!-- SECTION:FINAL_SUMMARY:END -->
