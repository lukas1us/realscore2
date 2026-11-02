---
id: TASK-39
title: Record price from search results on every run
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T17:32:00Z'
updated_date: '2026-11-02T21:02:00Z'
labels:
  - scraping
milestone: m-7
dependencies: []
type: enhancement
ordinal: 49000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Search items already include `price_summary_czk`, but a price change is only seen when the detail is refetched after `realscore.scrape.refetch-after` (7 days by default). Price drops are then late and dated wrong.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every run compares each search result's price with the last one seen
- [x] #2 If the search price differs from the latest known price, the detail is refetched in the same run, not after refetch-after
- [x] #3 A test shows a price change is visible after one run, not after 7 days
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Agreed 2026-11-02: no search price history; the search price only triggers a refetch, and price history stays derived from detail snapshots.
1. V12 migration: listing.search_price_czk NUMERIC NULL (raw price_summary_czk of the last search result taken into account; 0 = price on request, NULL = none recorded yet).
2. scraping: SrealityClient reads price_summary_czk per search result into SearchPage; SrealityIdCollector returns id -> search price (LinkedHashMap, order first seen).
3. listing: Listing.searchPriceCzk; ListingSnapshotService.findSearchPrices(source, ids) and recordSearchPrices(source, prices).
4. SrealityBulkCollector: a known, not-due listing whose stored search price is set and differs is refetched in the same run. After fetching, store the search price of every found listing, except those whose fetch failed or was skipped, so they are retried next run. BulkCollectionResult gains priceChanged; logged, not added to scrape_run.
5. Tests: client parses price; id collector returns prices; bulk collector: changed price refetched in the same run with the new price point dated to that run (AC3), unchanged not refetched, no stored price only recorded, failed refetch retried next run.
6. README operations notes. mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
V12 adds listing.search_price_czk (raw price_summary_czk; 0 = on request, NULL = none yet). Collector compares each found listing's search price with the stored one; a known, not-due listing with a set and different price is refetched in the same run. Search prices are stored after fetching, only when different, and not for skipped/failed fetches. AC3: SrealityScrapeJobTest runs the job twice within refetch-after; the 2nd run refetches the repriced listing and its new price point is dated to that run. mvn verify: 245 tests, BUILD SUCCESS.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
A price change in the search results now triggers a detail refetch in the same run, so price drops are dated to the run that saw them instead of up to refetch-after (7 days) late. listing.search_price_czk (V12) holds the last search price; price history is still derived only from detail snapshots. A failed or skipped refetch keeps the old search price, so it is retried next run. BulkCollectionResult gains priceChanged (logged, not in scrape_run). The first run after upgrading only stores search prices. Tests cover client parsing, collector prices, same-run refetch (AC3), unchanged price, no stored price, price on request, and retry after failure. README updated.
<!-- SECTION:FINAL_SUMMARY:END -->
