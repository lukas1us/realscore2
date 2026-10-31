---
id: TASK-14
title: ID collector with region/disposition bucketing
status: To Do
assignee: []
created_date: '2026-10-30T23:43:00Z'
labels:
  - scraping
milestone: m-3
dependencies:
  - TASK-13
documentation:
  - doc-1
ordinal: 14000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Beats the ~1000-result cap for a sale-apartment search bounded by a configurable price cap, following the region-then-disposition bucketing strategy documented in the 'Sreality API' doc: probe all 14 regions, subdivide any region whose result_size exceeds a safety threshold by the 11 disposition codes, then paginate every resulting bucket and dedupe IDs across all of them. Scoped to sale apartments with a price cap for now; removing the price cap or extending to other categories is tracked separately as later work, not part of this task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a price cap, the collector probes all 14 regions and, for any region over the bucket threshold, further probes all 11 dispositions within that region
- [ ] #2 IDs are collected by paginating every resulting bucket (small regions directly, large regions per-disposition) and deduplicated across buckets
- [ ] #3 A test using fixtures simulating one small region (collected directly) and one large region (subdivided by disposition, including an overlapping ID in two buckets) asserts the correct deduplicated ID set
<!-- AC:END -->
