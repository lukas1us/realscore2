---
id: TASK-42
title: Fall back to city when citypart is empty
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T17:32:00Z'
updated_date: '2026-11-01T19:27:00Z'
labels:
  - analysis
milestone: m-7
dependencies: []
type: bug
ordinal: 52000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Locality benchmarks use the city part for large cities. The parser stores citypart as given, so an empty string is not null, and Locality.finest builds a CITY_PART locality with an empty name. Those listings fall out of their city's benchmark into a bogus empty-name bucket.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A listing with an empty (or blank) citypart is still counted in its city benchmark
- [x] #2 It is not counted in a city-part benchmark with an empty name
- [x] #3 A test covers it
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Locality.finest: build a CITY_PART locality only when cityPart is non-blank (cityPart != null && !cityPart.isBlank()); an empty or blank city part falls through to the CITY locality, like null does. Fixed where the locality is built, not in the parser, so no reparse is needed and every caller (benchmark endpoints, MarketBenchmarks.forListing, scoring) gets it.
2. New LocalityTest (analysis/internal, plain unit test): empty and blank city part in a large city -> CITY locality with cityPart null; non-blank -> CITY_PART; existing fallbacks unchanged.
3. MarketBenchmarkServiceTest integration test: in a large city, one listing in 'Střed' and one with citypart '' -> no benchmark row has an empty city part name, and the city benchmark counts both (sample size 2).
4. No README change: endpoints, configuration and documented behaviour stay the same.
5. mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Local data on 2026-11-01: 791 listing_detail rows have citypart '' (Praha 97, Brno 31, Ostrava 11 among the large cities); none are NULL or whitespace-only. Fixed in Locality.finest (blank city part = missing), so no reparse is needed. New LocalityTest plus MarketBenchmarkServiceTest.anEmptyCityPartCountsInTheCityAndGetsNoCityPartBenchmarkOfItsOwn; both fail with the fix reverted. mvn verify: 179 tests, 0 failures.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Locality.finest now treats an empty or blank city part like a missing one, so a large-city listing with citypart '' gets its city's locality instead of a CITY_PART locality with an empty name. It counts in its city benchmark and no empty-name city-part bucket is produced, across the benchmark endpoints, forListing and the score. The parser still stores '' as given; no reparse or README change is needed. Covered by LocalityTest (null, empty, blank, normal, fallbacks) and a MarketBenchmarkServiceTest integration test; mvn verify passes.
<!-- SECTION:FINAL_SUMMARY:END -->
