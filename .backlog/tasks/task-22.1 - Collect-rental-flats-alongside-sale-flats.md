---
id: TASK-22.1
title: Collect rental flats alongside sale flats
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T12:41:00Z'
updated_date: '2026-11-01T14:16:00Z'
labels:
  - scraping
milestone: m-6
dependencies: []
documentation:
  - doc-1
parent_task_id: TASK-22
type: feature
ordinal: 34000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Market analysis (TASK-22) needs rent benchmarks per locality/disposition, but the Sreality scrape only searches sale flats (category_type_cb=1, hard-coded in SrealityIdCollector). The old app looked rents up live at analysis time; here raw data must be stored first and analysis cannot call scraping, so rentals go through the same scrape -> snapshot -> parse pipeline. The sale price cap (realscore.scrape.price-cap, total CZK) is meaningless for monthly rents. See doc-1 "Rental benchmark lookups".
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Each scrape run collects rental flats (category_type_cb=2) as well as sale flats, stored as listings and snapshots like sales
- [x] #2 Rentals use their own optional price cap, configurable separately from the sale price cap; with none set, rentals are not price-capped
- [x] #3 Removed-listing detection and refetching work for rentals the same as for sales
- [x] #4 Tests cover rental collection with WireMock fixtures
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. SrealityIdCollector: replace collectSaleApartmentIds(long) with collectSaleApartmentIds(long priceCap) + collectRentalApartmentIds(Long priceCap /* null = no price_to */), both delegating to one private bucketed collect(baseFilters); bucketing/paging unchanged.
2. ScrapeJobProperties: add rentPriceCap (nullable Long, @Positive when set) bound from realscore.scrape.rent-price-cap=${SCRAPE_RENT_PRICE_CAP:} (empty = uncapped). Existing priceCap stays the sale cap (key and env var unchanged), javadoc clarified.
3. SrealityBulkCollector.collect(salePriceCap, rentPriceCap, refetchAfter): found = union of sale and rental IDs, then the existing new/due fetch loop unchanged; log sale/rental found counts. Rental and sale estates share source "sreality", so removal detection (recordPresence over the union) and refetching apply to rentals unchanged.
4. SrealityScrapeJob: pass rentPriceCap; log it.
5. Tests: IdCollectorTest - rental search sends category_type_cb=2 and no price_to when uncapped, price_to when capped; BulkCollectorTest - sale and rental searches returning distinct IDs both get persisted; ScrapeJobTest - a rental missing from a run is marked removed, a due rental is refetched. Existing tests adjusted to the new signatures.
6. Update javadocs mentioning "sale apartments"; mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Rentals and sales share source "sreality"; the bulk collector unions both ID sets before the fetch loop and recordPresence, so removal/refetch need no change. Existing test search stubs now match category_type_cb=1 so the rental search falls through to the empty stub. Not verified live: that price_to on a rental search caps the monthly rent (only relevant if SCRAPE_RENT_PRICE_CAP is set).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Each scrape run now collects rental flats (category_type_cb=2) as well as sale flats. SrealityIdCollector gained collectRentalApartmentIds(Long priceCap), sharing the region/disposition bucketing with sales; null cap omits price_to. New optional realscore.scrape.rent-price-cap (env SCRAPE_RENT_PRICE_CAP, empty = uncapped); the sale cap keeps its key. SrealityBulkCollector.collect takes both caps and unions sale and rental IDs, so new/refetch/removed handling covers rentals unchanged. Tests: rental search params capped/uncapped (WireMock), rentals persisted alongside sales, a rental missing from a run is marked removed and a due rental is refetched, the empty default cap sends no price_to. mvn verify passes. Risk: first run after deploy fetches every rental detail nationally (thousands of requests at 0.5 s).
<!-- SECTION:FINAL_SUMMARY:END -->
