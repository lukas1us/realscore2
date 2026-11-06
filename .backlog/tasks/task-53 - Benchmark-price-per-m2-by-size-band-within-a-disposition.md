---
id: TASK-53
title: Benchmark price per m2 by size band within a disposition
status: To Do
assignee: []
created_date: '2026-11-06T13:55:00Z'
labels: []
dependencies: []
priority: low
type: enhancement
ordinal: 66000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Sale and rent medians are grouped by locality and disposition only. Within one disposition the price per m² falls as size grows, so a small 2+kk looks expensive and a large one cheap against the same median, which biases the discount part of the score. Grouping by size band as well, with a fallback to the disposition alone where a band has too few listings, should make the comparison fairer. Check the effect on the real data (e.g. in Metabase, TASK-25.2) before choosing band limits.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Benchmarks can be grouped by configurable size bands within a disposition
- [ ] #2 A band with too few listings falls back to the whole disposition, then to coarser localities as today
- [ ] #3 The benchmark endpoints and the score response show which band (if any) a median is for
- [ ] #4 Tests cover band grouping and the fallback
- [ ] #5 README.md documents the bands and their settings
<!-- AC:END -->
