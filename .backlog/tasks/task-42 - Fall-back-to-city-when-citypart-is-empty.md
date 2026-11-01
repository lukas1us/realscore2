---
id: TASK-42
title: Fall back to city when citypart is empty
status: To Do
assignee: []
created_date: '2026-11-01T17:32:00Z'
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
- [ ] #1 A listing with an empty (or blank) citypart is still counted in its city benchmark
- [ ] #2 It is not counted in a city-part benchmark with an empty name
- [ ] #3 A test covers it
<!-- AC:END -->
