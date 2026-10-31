---
id: TASK-12
title: Wire Sreality client output into listing and snapshot on fetch
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-31T13:04:00Z'
labels:
  - scraping
  - listing
milestone: m-2
dependencies:
  - TASK-10
  - TASK-11
ordinal: 12000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Connects the two previous tasks end to end for a single estate ID: fetch its detail, then persist/update the listing and append a snapshot if the payload changed. This is the smallest possible vertical slice through scraping -> listing and the base every later milestone (bulk collection, scheduling) builds on.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Fetching a not-yet-seen estate ID creates a new listing row and its first snapshot
- [x] #2 Re-fetching the same estate ID with an unchanged payload updates last_seen_at but writes no new snapshot
- [x] #3 Re-fetching the same estate ID with a changed payload updates the listing and writes a new snapshot
- [x] #4 An integration test (WireMock fixture + Testcontainers Postgres) covers all three cases
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. scraping.internal.SrealityEstateFetcher (@Component): fetch(long estateId) -> SrealityClient.fetchEstateDetail -> ListingSnapshotService.recordFetch("sreality", id, body); SrealityApiException propagates, nothing written
2. SrealityEstateFetcherTest: @SpringBootTest + PostgresContainerConfig + WireMock (@DynamicPropertySource for realscore.sreality.base-url); asserts via JdbcTemplate (no listing.internal access); distinct estate id per test
   - new id -> 1 listing + 1 snapshot equal to fixture JSON
   - unchanged refetch -> still 1 snapshot, last_seen_at advanced
   - changed refetch (price_czk rewritten) -> 2 snapshots, last_seen_at advanced
3. mvn verify incl. ModularityTests
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Added scraping.internal.SrealityEstateFetcher: fetch(estateId) = SrealityClient.fetchEstateDetail + ListingSnapshotService.recordFetch("sreality", id, body); uses only listing's public API. SrealityApiException propagates and nothing is written (removal tracking is TASK-19). SrealityEstateFetcherTest: @SpringBootTest + Testcontainers Postgres + WireMock via @DynamicPropertySource; asserts via JdbcTemplate so the test doesn't touch listing.internal. Covers new estate (1 listing, 1 snapshot equal to fixture as jsonb), unchanged refetch (1 snapshot, last_seen_at advanced), changed refetch (price_czk rewritten -> 2 snapshots, last_seen_at advanced). mvn verify: 11 tests, BUILD SUCCESS, ModularityTests green. 'Updates the listing' in AC #3 = last_seen_at (listing has no payload-derived columns yet). Follow-up: TASK-28 (volatile fields in live payload may defeat hash dedup).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Connected scraping to listing end to end for one estate: SrealityEstateFetcher (scraping.internal) fetches the v1 detail via SrealityClient and records it through listing's public ListingSnapshotService.recordFetch("sreality", id, body). API errors propagate as SrealityApiException with nothing written (removal tracking is TASK-19). SrealityEstateFetcherTest runs WireMock + real client/service + Testcontainers Postgres and asserts via SQL (no listing.internal access): new estate -> 1 listing + 1 snapshot equal to the fixture; unchanged refetch -> still 1 snapshot, last_seen_at advanced; changed price -> 2 snapshots, last_seen_at advanced. 'Updates the listing' means last_seen_at, as listing has no payload-derived columns yet. mvn verify green (13 tests, isolated worktree at a83ddc1 + this change). Follow-up: TASK-28 (live payload may contain per-request volatile fields that defeat hash dedup).
<!-- SECTION:FINAL_SUMMARY:END -->
