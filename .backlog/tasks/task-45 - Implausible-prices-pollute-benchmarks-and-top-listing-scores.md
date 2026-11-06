---
id: TASK-45
title: Implausible prices pollute benchmarks and top listing scores
status: Done
assignee:
  - '@claude'
created_date: '2026-11-05T11:48:00Z'
updated_date: '2026-11-06T21:16:00Z'
labels:
  - parsing
  - analysis
dependencies: []
priority: high
type: bug
ordinal: 58000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Sellers miscategorise listings or use placeholder prices, and those listings currently go straight into the medians and scores. Examples from the DB on 2026-11-05: sale flat 1741094988 (Mariánské Lázně, 56 m²) priced 12,500 CZK with `price_unit_cb` "za měsíc" (a rent listed as a sale) is #3 in its city with total score 85.5, discount 99.6 % and rental yield 1086 %. Two Litvínov sales at 134,500 and 269,000 CZK score 81.8 with 67 % yield. Rentals priced at 1 CZK (Most) or 300 CZK (5× Šestajovice, `price_czk_m2` 6), and rentals at 6,893,000 / 870,000 / 780,000 CZK a month (sale prices listed as rent). There are also sizes of 8–11 m² for 1+kk sales at 1.3–2.5M and 575–5731 m² for ordinary rentals. `price_unit_cb` alone is not reliable either: 85 rentals marked "za nemovitost" have normal monthly rents (median 17,000), and one "za měsíc" sale is priced at 2.8M. Medians resist a few outliers, but the per-listing discount and yield do not, so these rise to the top of `/api/analysis/scores`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A sale or rental whose price or size is implausible is excluded from sale and rent benchmarks
- [x] #2 Such a listing gets no score, or is clearly flagged, instead of ranking near the top of /api/analysis/scores
- [x] #3 The examples in the description (12,500 CZK sale, 1 CZK and 300 CZK rents, 6.9M CZK rent) are all treated as implausible, and ordinary listings are not
- [x] #4 The plausibility thresholds are configurable and documented in README.md
- [x] #5 Raw snapshots are not changed; the rule is re-applied to existing data by a reparse or on demand
- [x] #6 Tests cover sale and rent cases on both sides of each threshold
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. PlausibilityProperties (analysis/internal, realscore.analysis.plausibility.{sale,rent}): min/max price, min/max price per m2, min/max size; each bound optional. Defaults: sale price >= 200000, 5000-400000 CZK/m2, 12-400 m2; rent 1000-300000 CZK/month, 50-2000 CZK/m2/month, 12-400 m2.
2. Plausibility (analysis/internal): isPlausible(price, size, bounds); a bound only applies to a value that is present.
3. MarketBenchmarkService: drop implausible sales and rentals before building samples (sale and rent benchmarks, benchmarks()).
4. ListingScoreService: implausible sale flats are not scorable (scoreAll/top and the single-listing score -> null).
5. Computed on demand from listing_detail, so snapshots stay untouched and a config change applies at once (no reparse).
6. Tests: both sides of every threshold for sale and rent, the task examples (12,500 sale, 1 and 300 CZK rents, 6.9M rent) implausible and ordinary listings kept; benchmark and score service tests exclude them. README: property table and behaviour.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Added PlausibilityProperties (realscore.analysis.plausibility.{sale,rent}.{min-price,max-price,min-price-per-m2,max-price-per-m2,min-size,max-size}); every bound inclusive and optional (empty = none), and applies only to values the listing has. Defaults: sale >= 200,000 CZK, 5,000-400,000 CZK/m2, 12-400 m2, no max price (the scrape cap applies); rent 1,000-300,000 CZK/month, 50-2,000 CZK/m2, 12-400 m2. MarketBenchmarkService drops implausible sales/rentals before building samples (sale, rent and the per-listing lookup); ListingScoreService treats implausible sales as not scorable (scores list and single-listing score -> null). Applied on demand to listing_detail, so snapshots stay raw and a config change applies without reparse. price_unit_cb deliberately not used (unreliable per the task description).
Against the live DB on 2026-11-06 the defaults exclude 12 of 8,507 active sale flats and 21 of 11,790 rentals.
Tests: PlausibilityPropertiesTest (both sides of each threshold for sale and rent, the task's examples, missing values, validation, binding of application.properties), MarketBenchmarkServiceTest.implausibleSalesAndRentalsAreLeftOut, ListingScoreServiceTest.anImplausibleSaleIsNotScored. README: property rows and an Operations note. mvn verify green (320 tests).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Listings with an implausible price, price per m2 or size (configurable bounds under realscore.analysis.plausibility) are now left out of sale and rent benchmarks and get no score, so miscategorised or placeholder listings no longer top /api/analysis/scores. Applied on demand, raw data untouched. Defaults exclude 12 sales and 21 rentals in the current data. Covered by threshold and example tests; README documents the bounds.
<!-- SECTION:FINAL_SUMMARY:END -->
