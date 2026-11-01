---
id: TASK-22.3
title: Median sale price per m2 by locality and disposition
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T12:41:00Z'
updated_date: '2026-11-01T12:53:00Z'
labels:
  - analysis
dependencies:
  - TASK-22.2
documentation:
  - doc-1
parent_task_id: TASK-22
type: feature
ordinal: 36000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
First real analysis feature: a sale price benchmark (median CZK/m2) per locality and disposition, replacing the old app's approach, so individual listings can later be compared against their market (scoring, TASK-24). Decided when TASK-22 was split: compute on demand from listing_detail (no stored benchmark table; benchmarks over time are a later task next to TASK-23); locality is city part for large cities and city elsewhere, falling back to district and then region when a group is too small. Caveat: sales are scraped only below realscore.scrape.price-cap (5M CZK by default), so medians in expensive areas (e.g. Praha) describe the cheap end of the market, which is why sample counts must be visible.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Medians are computed over active (not removed) sale flats that have both a price and a size
- [x] #2 Each result gives locality, disposition, median CZK/m2 and the number of listings it is based on
- [x] #3 Large cities (configurable list, default Praha, Brno, Ostrava) are grouped by city part, other cities by city
- [x] #4 A group below a configurable minimum sample size falls back to district, then region; the result states which locality level was used, and groups still too small are omitted
- [x] #5 A read-only GET endpoint returns the results
- [x] #6 Tests on Testcontainers Postgres cover the median, the exclusions, and the fallback
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. parsing (public API): ListingDetailsQueryService.findActive(transactionTypeCode, propertyCategoryCode) -> List<ActiveListingDetails>, JdbcClient join of listing_detail with listing.removed_at IS NULL, so analysis never touches parsing/listing internals.
2. analysis.internal: Locality (path region/district/city/cityPart + level, finest(), parent()), MedianBenchmarks (each sample counts towards its finest locality and every enclosing one; each finest locality takes the first group walking outwards with >= minSampleSize; median in Java), AnalysisProperties (large-cities, min-sample-size) + configuration.
3. analysis: LocalityLevel, SalePriceBenchmark record, MarketBenchmarkService.salePricePerM2(), GET /api/analysis/benchmarks/sale-price-per-m2.
4. Tests: MedianBenchmarksTest (unit: median, grouping, each fallback level, omission), MarketBenchmarkServiceTest (Testcontainers: exclusions, city-part fallback, endpoint). mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Median computed in Java rather than with percentile_cont as suggested when scoping: the walk-outwards fallback is much simpler in code than in SQL, and the data volume (tens of thousands of active rows) is fine in memory. A fallback benchmark uses all listings of the enclosing locality, not only the leftovers of small groups. Defaults: large cities Praha, Brno, Ostrava; min sample 5. Localities are keyed by full path so same-named cities in different districts stay apart. Not verified against live data: how Sreality fills citypart for Praha/Brno/Ostrava.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added GET /api/analysis/benchmarks/sale-price-per-m2: median asking CZK/m2 of active sale flats per (locality, disposition), computed on each request. Large cities (realscore.analysis.large-cities, default Praha,Brno,Ostrava) are grouped by city part, others by city; below realscore.analysis.min-sample-size (default 5) a group falls back to city, district, region, and each result states the level used and its sample size; groups too small everywhere are omitted. Parsing gained a public ListingDetailsQueryService so analysis reads only via module APIs (ModularityTests passes). Tests: MedianBenchmarksTest (7) and MarketBenchmarkServiceTest (3, Testcontainers) cover the median, exclusions (removed, rental, non-flat, no price, no size) and fallback. mvn verify passes. Caveat: sales are only scraped below the 5M sale price cap, so medians in expensive areas describe the cheap end.
<!-- SECTION:FINAL_SUMMARY:END -->
