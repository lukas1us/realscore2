---
id: TASK-52
title: Weigh the score by how reliable its benchmark is
status: To Do
assignee: []
created_date: '2026-11-06T13:55:00Z'
labels: []
dependencies: []
priority: medium
type: enhancement
ordinal: 65000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The discount and rental-yield parts compare a listing with a median that may rest on as few as `realscore.analysis.min-sample-size` (5) listings, or that may fall back from the city part to the region. A discount against a 5-listing regional median currently counts the same as one against 200 listings in the same city part, so thin or coarse benchmarks can push listings to the top. The sample size and fallback level are already known in `MedianBenchmarks.Benchmark`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A part based on a smaller sample or a coarser fallback locality has less influence on the total than one based on a large local sample
- [ ] #2 The score response shows, for each benchmark-based part, its sample size and the locality level the median came from
- [ ] #3 The adjustment is configurable, and a setting reproduces the current behaviour
- [ ] #4 Tests cover small vs large samples and local vs fallback benchmarks
- [ ] #5 README.md explains the adjustment and its settings
<!-- AC:END -->
