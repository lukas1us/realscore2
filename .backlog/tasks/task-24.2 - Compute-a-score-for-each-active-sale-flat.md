---
id: TASK-24.2
title: Compute a score for each active sale flat
status: To Do
assignee: []
created_date: '2026-11-01T16:20:00Z'
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
- [ ] #1 Discount = (median CZK/m2 - listing CZK/m2) / median; yield = expected rent x 12 / price, with expected rent = rent-per-m2 median x size, or the monthly rent median when no per-m2 median exists; drop = percent drop over the price-drop window
- [ ] #2 Each part maps linearly to a 0-100 sub-score between configurable limits (defaults: discount -30%..+30%, yield 2%..8%, drop 0%..15%), clamped at the ends
- [ ] #3 The total is the weighted average of the available sub-scores with configurable weights (defaults 50/35/15); a part without data (e.g. no rent benchmark) is left out and the remaining weights are rescaled
- [ ] #4 Each score includes the total, every sub-score with its raw value, and the benchmark level and sample size used
- [ ] #5 Unit tests cover each part, clamping, a missing part and the weighting
<!-- AC:END -->
