---
id: TASK-47
title: Listings that leave the search scope stay active and are refetched every run
status: To Do
assignee: []
created_date: '2026-11-05T11:48:00Z'
labels:
  - scraping
  - analysis
dependencies: []
priority: low
type: bug
ordinal: 60000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Since TASK-38 an unseen listing is marked removed only when its detail returns 404/410. A listing that is still online but dropped out of the capped search (price raised above `SCRAPE_PRICE_CAP`, or switched to price on request) therefore stays active indefinitely. Its detail is also fetched again by the removal check on every run, so that cost grows with every such listing. On 2026-11-05 there were 17: 12 sale listings now without a price, 2 sales at 5,210,000 CZK above the 5M cap, and 3 rentals. The two above-cap sales still count as active sale flats in the benchmarks and scores, although the collected sample otherwise ends at the cap.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A listing known to be online but outside the collected scope is distinguishable from an in-scope active listing
- [ ] #2 Such listings are not refetched on every run; there is a bounded, documented recheck cadence
- [ ] #3 Benchmarks and scores do not include sale listings above the current price cap
- [ ] #4 A listing that returns to scope is treated as active again
- [ ] #5 Tests cover going out of scope, staying out, and returning; README.md documents the behaviour
<!-- AC:END -->
