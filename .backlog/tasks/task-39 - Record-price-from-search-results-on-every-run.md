---
id: TASK-39
title: Record price from search results on every run
status: To Do
assignee: []
created_date: '2026-11-01T17:32:00Z'
labels:
  - scraping
milestone: m-7
dependencies: []
type: enhancement
ordinal: 49000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Search items already include `price_summary_czk`, but a price change is only seen when the detail is refetched after `realscore.scrape.refetch-after` (7 days by default). Price drops are then late and dated wrong.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Every run records a price point from each search result
- [ ] #2 If the search price differs from the latest known price, the detail is refetched in the same run, not after refetch-after
- [ ] #3 A test shows a price change is visible after one run, not after 7 days
<!-- AC:END -->
