---
id: TASK-23.2
title: Single listing endpoint with price history
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T14:16:00Z'
updated_date: '2026-11-01T15:52:00Z'
labels:
  - analysis
milestone: m-6
dependencies:
  - TASK-23.1
parent_task_id: TASK-23
type: feature
ordinal: 40000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
There is no way to look at one listing through the app; today it needs SQL against Postgres. Seeing a listing together with its price history is the basic view needed before scoring or a UI.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 GET /api/listings/{source}/{externalId} returns the parsed details, first seen, last seen, removed time (if any) and the price history ordered by time
- [x] #2 The response includes days on market, number of price drops, and percent change from the first to the current price
- [x] #3 An unknown listing returns 404
- [x] #4 README documents the endpoint
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. parsing: ListingDetailsQueryService.findListing(source, externalId) -> Optional<ListingOverview> (new public record in parsing): listing timestamps (first/last seen, removed) joined via SQL like findActive, the listing_detail fields (null if never parsed), and the price history (new public record PricePoint) ordered by observed_at.
2. analysis: ListingOverviewService adds the derived figures (new record ListingStats): days on market = whole days from first_seen_at to removed_at or now; price drops = points lower than the previous one; percent change = (last - first) / first * 100, 1 decimal, null with no price history.
3. analysis: ListingController GET /api/listings/{source}/{externalId} -> ListingResponse (overview + stats); unknown listing -> 404 via ResponseStatusException.
4. Tests: query service (unknown, not parsed yet, full); stats unit test (no history, drops, rise, removed vs active days); endpoint test for 200 and 404.
5. README: endpoint row and a note that days on market counts from the first time realscore saw the listing.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Deviation from plan step 1: listing timestamps come from a new listing-module API (ListingSnapshotService.findListing -> ListingView) instead of a SQL join; details and history are read through the parsing JPA repositories. Keeps listing internals behind its API. ListingPriceHistoryUpdater now reuses the public PricePoint record. mvn verify: 116 tests, BUILD SUCCESS.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Adds GET /api/listings/{source}/{externalId}. parsing exposes ListingDetailsQueryService.findListing -> ListingOverview (presence timestamps, ListingDetails or null, PricePoint history); listing exposes findListing -> ListingView. analysis adds ListingOverviewService, ListingStats (days on market from first seen to removal/now, price drops, % change first->current, 1 decimal) and ListingController (404 when unknown). Tests: ListingStatsTest (6), ListingControllerTest (3), findListing test. README: endpoint row and a note that days on market count from first seen, as Sreality has no publication date.
<!-- SECTION:FINAL_SUMMARY:END -->
