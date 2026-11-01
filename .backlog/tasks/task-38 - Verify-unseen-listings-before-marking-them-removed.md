---
id: TASK-38
title: Verify unseen listings before marking them removed
status: To Do
assignee: []
created_date: '2026-11-01T17:32:00Z'
labels:
  - scraping
milestone: m-7
dependencies: []
type: bug
ordinal: 48000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A listing that drops out of the search results is marked removed, but that also happens to live listings, e.g. when the price rises above `realscore.scrape.price-cap` or the category changes. The removal data and the price history are then wrong.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 After a successful run, the detail is fetched for every listing that run did not see
- [ ] #2 Detail 404/410 sets removed_at
- [ ] #3 Detail 200 records the snapshot and keeps the listing active
- [ ] #4 A test covers a listing whose price rises above the price cap: it stays active and its new price is recorded
<!-- AC:END -->
