---
id: TASK-22.4
title: Median rent by locality and disposition
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T12:41:00Z'
updated_date: '2026-11-01T14:16:00Z'
labels:
  - analysis
milestone: m-6
dependencies:
  - TASK-22.1
  - TASK-22.3
documentation:
  - doc-1
parent_task_id: TASK-22
type: feature
ordinal: 37000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Rent benchmark per locality and disposition, the second half of TASK-22, needed later to estimate achievable rent for a listing (rental yield itself belongs to scoring, TASK-24). Uses the rentals collected by TASK-22.1 and the same locality grouping, fallback and minimum-sample rules as the sale benchmark (TASK-22.3), computed on demand.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Medians are computed over active (not removed) rental flats with a price
- [x] #2 Each result gives locality, disposition, median monthly rent in CZK, median rent CZK/m2 (over listings that also have a size) and the number of listings each is based on
- [x] #3 Locality grouping, fallback and minimum sample size behave the same as for the sale benchmark
- [x] #4 A read-only GET endpoint returns the results
- [x] #5 Tests on Testcontainers Postgres cover the medians, the exclusions, and that sale listings are not counted
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. analysis: BenchmarkValue(level, median, sampleSize), RentBenchmark (path, disposition, monthlyRentCzk, rentPerM2Czk).
2. MarketBenchmarkService.rent(): active rental flats with a price; two MedianBenchmarks runs (rent; rent/m2 over those with a positive size), joined per (locality, disposition); same grouping/fallback/min sample as sales.
3. GET /api/analysis/benchmarks/rent.
4. Tests in MarketBenchmarkServiceTest (Testcontainers): medians and exclusions incl. sales not counted, per-m2 empty when too few sizes, endpoint. mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Rent and rent/m2 fall back independently (each BenchmarkValue carries its own level and sample size), since fewer rentals have a size. Rent/m2 is null when even the region has too few sized rentals; a rent/m2 benchmark can never exist without a rent one, as its samples are a subset.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added GET /api/analysis/benchmarks/rent: per (locality, disposition) the median monthly rent and median rent CZK/m2 of active rental flats, each with its own fallback level and sample size; same locality grouping, fallback and minimum sample as the sale benchmark. Tests (Testcontainers): medians, exclusions (removed, sales, non-flats, no price), rent/m2 left empty when too few rentals have a size, endpoint. mvn verify passes (99 tests). Rental yield is left to scoring (TASK-24).
<!-- SECTION:FINAL_SUMMARY:END -->
