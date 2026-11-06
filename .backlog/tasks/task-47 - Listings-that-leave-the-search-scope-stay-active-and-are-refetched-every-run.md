---
id: TASK-47
title: Listings that leave the search scope stay active and are refetched every run
status: Done
assignee:
  - '@claude'
created_date: '2026-11-05T11:48:00Z'
updated_date: '2026-11-06T21:35:00Z'
labels:
  - scraping
  - analysis
dependencies: []
priority: low
type: bug
ordinal: 60000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Since TASK-38 an unseen listing is marked removed only when its detail returns 404/410. A listing that is still online but dropped out of the capped search (price raised above `SCRAPE_PRICE_CAP`, or switched to price on request) therefore stays active indefinitely. Its detail is also fetched again by the removal check on every run, so that cost grows with every such listing. On 2026-11-05 there were 17: 12 sale listings now without a price, 2 sales at 5,210,000 CZK above the 5M cap, and 3 rentals. The two above-cap sales still count as active sale flats in the benchmarks and scores, although the collected sample otherwise ends at the cap.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A listing known to be online but outside the collected scope is distinguishable from an in-scope active listing
- [x] #2 Such listings are not refetched on every run; there is a bounded, documented recheck cadence
- [x] #3 Benchmarks and scores do not include sale listings above the current price cap
- [x] #4 A listing that returns to scope is treated as active again
- [x] #5 Tests cover going out of scope, staying out, and returning; README.md documents the behaviour
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Migration (new-migration skill): listing.out_of_scope_since TIMESTAMPTZ NULL.
2. listing: Listing entity field; ListingView exposes outOfScopeSince. recordPresence clears it for seen listings (back in scope). New markOutOfScope(source, ids, now) sets it where null. findActiveExternalIdsNotSeenSince skips out-of-scope listings fetched after a recheck cutoff.
3. scraping: realscore.scrape.out-of-scope-recheck-after (SCRAPE_OUT_OF_SCOPE_RECHECK_AFTER, default 7d). SrealityRemovalCheck: HTTP 200 for an unseen listing marks it out of scope; 404/410 still removed; out-of-scope listings are only rechecked once their last fetch is older than the cadence.
4. parsing/analysis: ListingDetailsQueryService.findActive (benchmarks and scores) excludes out-of-scope listings, so sales above the price cap drop out. Listing endpoint shows outOfScopeSince.
5. Tests: going out of scope, staying out (not refetched before the cadence, refetched after), returning to scope; benchmarks exclude out-of-scope sales. README documents it.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
V14 adds listing.out_of_scope_since (SQL reviewed and approved). SrealityRemovalCheck marks unseen listings answering 200 out of scope (ListingSnapshotService.markOutOfScope keeps the first time); 404/410 still mark removed (and clear the mark). findActiveExternalIdsNotSeenSince skips out-of-scope listings fetched within realscore.scrape.out-of-scope-recheck-after (SCRAPE_OUT_OF_SCOPE_RECHECK_AFTER, default 7d), so they are rechecked on a bounded cadence instead of every run. recordPresence clears the mark when the search returns the listing again. ListingDetailsQueryService.findActive excludes out-of-scope listings, so benchmarks and scores only use the collected sample (sales above the cap drop out); ListingScoreService.score(listing) returns empty for them. ListingView, ListingOverview and the listing endpoint expose outOfScopeSince. No backfill: the next run flags existing cases.
Not changed: price drops (/api/analysis/price-drops) still include out-of-scope listings; not in the ACs.
Tests: SrealityScrapeJobTest (out of scope, not refetched on the next run, refetched after the interval, back in scope; out-of-scope listing gone at recheck -> removed), ListingSnapshotServiceTest (unseen query, mark keeps first time, cleared on presence/removal), MarketBenchmarkServiceTest.salesOutOfScopeAreLeftOut, ListingScoreServiceTest.aSaleOutOfScopeIsNotScored. README: config row, endpoint field, How it works, Operations note. mvn verify green (326 tests).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Listings still online but no longer returned by the searches are now marked out of scope (listing.out_of_scope_since, V14). They are left out of benchmarks and scores, rechecked only every SCRAPE_OUT_OF_SCOPE_RECHECK_AFTER (7d) instead of every run, removed on 404/410, and back in scope once a search returns them again. Exposed as outOfScopeSince in the listing endpoint. Covered by scrape, listing and analysis tests; README documents it. Follow-up to consider: price drops still include out-of-scope listings.
<!-- SECTION:FINAL_SUMMARY:END -->
