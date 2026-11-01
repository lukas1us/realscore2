---
id: TASK-24.1
title: Look up the sale and rent benchmarks for one listing
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T16:20:00Z'
updated_date: '2026-11-01T16:34:00Z'
labels:
  - analysis
milestone: m-6
dependencies: []
parent_task_id: TASK-24
type: feature
ordinal: 42000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Scoring a listing (TASK-24) compares it with the market, so each flat needs the benchmarks that apply to it: the median sale price per m2 and the median rents for its locality and disposition, with the same city-part grouping and coarser-locality fallback the benchmark endpoints use. Today benchmarks are only produced as whole lists for the endpoints.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given a flat with a locality and disposition, the matching sale benchmark and rent benchmarks are returned, including the level and sample size used
- [x] #2 A locality with too few listings falls back to the coarser level exactly as the benchmark endpoints do, and both give the same medians for the same input
- [x] #3 A flat without a disposition or locality, or with no benchmark even at region level, gets no benchmark rather than an error
- [x] #4 Benchmarks are computed once per request, not once per listing
- [x] #5 Existing benchmark endpoints return unchanged results and their tests still pass
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. analysis/internal MedianBenchmarks: add an index built once from the samples (values per (locality, disposition) at every level, median cached per group) with find(locality, dispositionCode) walking outwards to the first group with at least minSampleSize samples. Works for any locality, also one with no samples of its own (e.g. a sale flat in a city part with no rentals). Re-implement compute() on top of it so endpoints and lookups share one code path.
2. analysis: public MarketBenchmarks (built by new MarketBenchmarkService.benchmarks(), one computation per call) with forListing(region, district, city, citypart, dispositionCode) -> ListingBenchmarks(salePricePerM2, monthlyRent, rentPerM2), each a BenchmarkValue or null. Uses Locality.finest with large-cities, same sample filters as the endpoints.
3. salePricePerM2() and rent() keep their output; they share the sample-building code with benchmarks().
4. Tests: MedianBenchmarksTest for find (own group, fallback, locality without samples, nothing large enough, compute == find); MarketBenchmarkServiceTest for forListing (same medians as the endpoints, rent fallback for a locality without rentals, no disposition/locality -> all null). Existing tests unchanged.
5. No README change: no endpoint or configuration change.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
MedianBenchmarks.index builds the medians of every (locality, disposition) group at every level once; Index.find walks outwards from any locality. compute() now runs on top of the index, so endpoints and lookups share the fallback code (computeAgreesWithTheIndex test). MarketBenchmarkService.benchmarks() reads sales and rentals once and returns MarketBenchmarks; forListing is only map lookups, so a caller scoring many flats computes benchmarks once (AC #4 by construction, not by a test). Merged the identical sale and rent per-m2 sample builders into perM2Samples. Also wrapped an over-long Javadoc line in AnalysisProperties from TASK-23.3. Existing benchmark tests unchanged and passing. mvn verify: 137 tests, BUILD SUCCESS.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Adds per-flat benchmark lookup for scoring: MarketBenchmarkService.benchmarks() -> MarketBenchmarks.forListing(region, district, city, citypart, disposition) -> ListingBenchmarks(salePricePerM2, monthlyRent, rentPerM2), each a BenchmarkValue or null. Uses the same grouping, sample filters and fallback as the benchmark endpoints via the new MedianBenchmarks.Index, which also finds benchmarks for a locality with no listings of its own (e.g. rent for a city part without rentals). Tests: 4 new in MedianBenchmarksTest, 3 new in MarketBenchmarkServiceTest. No endpoint or config change, so no README change.
<!-- SECTION:FINAL_SUMMARY:END -->
