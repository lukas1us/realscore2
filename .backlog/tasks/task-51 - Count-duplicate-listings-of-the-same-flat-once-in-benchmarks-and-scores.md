---
id: TASK-51
title: Count duplicate listings of the same flat once in benchmarks and scores
status: To Do
assignee: []
created_date: '2026-11-06T13:55:00Z'
labels: []
dependencies:
  - TASK-49
priority: medium
type: feature
ordinal: 64000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The same flat is often listed by several agencies. Each listing is a separate sample in the sale and rent medians and a separate entry in `/api/analysis/scores`, which skews medians in small localities and crowds the top scores with copies. TASK-25.2 asks for a "suspected duplicates" view in Metabase, which is a good place to tune the matching rule (e.g. same disposition, size within about 1 m², same floor, nearby GPS from TASK-49) before it goes into code. Raw listings stay separate: duplicates are a derived grouping, not a merge.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Listings that are very likely the same flat are grouped, and the matching rule is documented
- [ ] #2 Each group counts once in sale and rent benchmarks
- [ ] #3 Each group appears once in /api/analysis/scores, and the response shows the other listings of the group
- [ ] #4 Listings are never merged or deleted; the grouping can be recomputed
- [ ] #5 Tests cover true duplicates and look-alikes that must stay separate (e.g. same building, different floor)
- [ ] #6 README.md documents the grouping and its effect on benchmarks and scores
<!-- AC:END -->
