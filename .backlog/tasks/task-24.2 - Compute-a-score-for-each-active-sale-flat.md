---
id: TASK-24.2
title: Compute a score for each active sale flat
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T16:20:00Z'
updated_date: '2026-11-01T16:47:00Z'
labels:
  - analysis
milestone: m-6
dependencies:
  - TASK-24.1
parent_task_id: TASK-24
type: feature
ordinal: 43000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The point of realscore is to rank listings by how good a deal they are. v1 combines three signals the app already has: discount against the local sale median, gross rental yield from local rent medians, and a recent price drop (TASK-23.3). Only active flats for sale with a price, size, disposition and locality are scored. Quality factors are out of scope (separate task).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Discount = (median CZK/m2 - listing CZK/m2) / median; yield = expected rent x 12 / price, with expected rent = rent-per-m2 median x size, or the monthly rent median when no per-m2 median exists; drop = percent drop over the price-drop window
- [x] #2 Each part maps linearly to a 0-100 sub-score between configurable limits (defaults: discount -30%..+30%, yield 2%..8%, drop 0%..15%), clamped at the ends
- [x] #3 The total is the weighted average of the available sub-scores with configurable weights (defaults 50/35/15); a part without data (e.g. no rent benchmark) is left out and the remaining weights are rescaled
- [x] #4 Each score includes the total, every sub-score with its raw value, and the benchmark level and sample size used
- [x] #5 Unit tests cover each part, clamping, a missing part and the weighting
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. parsing: add source and externalId to ActiveListingDetails (selected in findActive), so scores can be identified and joined with price drops.
2. analysis/internal: ScoringProperties under realscore.analysis.scoring.{discount,rental-yield,price-drop}.{min,max,weight}, defaults -30/30/50, 2/8/35, 0/15/15, validated (max > min, weight >= 0).
3. analysis/internal: ScoreCalculator, pure: raw values (discount %, yield %, drop %) -> sub-scores 0-100 linear between min and max, clamped; total = weighted average of the parts that have data, weights rescaled. Raw values 2 decimals, scores 1 decimal.
4. analysis: ListingScore record (source, externalId, disposition, locality, price, size, total, and discount / rentalYield / priceDrop as ScorePart(rawPercent, score, weight, benchmark) or null when missing).
5. analysis: ListingScoreService.scoreAll(): one MarketBenchmarks, active sale flats with price, positive size, disposition and region, drops from PriceDropService over the price-drop window joined by (source, externalId); sorted by total desc.
6. Rules: no drop in the window = 0 % drop (sub-score 0), not missing. A flat with neither discount nor yield data is not scored (drop alone says nothing about the deal).
7. Tests: ScoreCalculatorTest (each part, clamping both ends, missing part rescales weights, weighting, drop-only not scored); ListingScoreServiceTest (only active sale flats, drop joined, yield from rent-per-m2 vs monthly fallback).
8. README: scoring properties in the configuration table (no endpoint yet; TASK-24.3).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented as planned. ScoreCalculator (analysis/internal, pure) takes raw percentages; sub-scores are rounded to 1 decimal before weighting so the total can be recomputed from the shown values. ScorePart.weightPercent is the share after rescaling. Also returns empty when the known parts all weigh 0 (avoids division by zero). Expected rent prefers rent-per-m2 median x size, else monthly median; the ScorePart benchmark is the one used. ActiveListingDetails gained source and externalId (findActive selects them). Discount is against a median that includes the flat itself. ScoringPropertiesTest covers the max > min and weight >= 0 validation. mvn verify: 151 tests, BUILD SUCCESS.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Adds the v1 listing score, computed on demand: ListingScoreService.scoreAll() scores active sale flats (price, size > 0, disposition, region) from discount vs the local sale median per m2, gross rental yield from local rent medians, and the price drop over the price-drop window (0 without one), best first. ScoreCalculator maps each to 0-100 between configurable limits (clamped) and takes the weighted average of the parts with data; flats with neither discount nor yield are not scored. Config realscore.analysis.scoring.{discount,rental-yield,price-drop}.{min,max,weight}, validated. Tests: ScoreCalculatorTest (7), ListingScoreServiceTest (5), ScoringPropertiesTest (2). README: scoring config rows. No endpoint yet (TASK-24.3).
<!-- SECTION:FINAL_SUMMARY:END -->
