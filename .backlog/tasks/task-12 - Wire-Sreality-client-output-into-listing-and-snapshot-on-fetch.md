---
id: TASK-12
title: Wire Sreality client output into listing and snapshot on fetch
status: To Do
assignee: []
created_date: '2026-10-30T23:42:00Z'
labels:
  - scraping
  - listing
milestone: m-2
dependencies:
  - TASK-10
  - TASK-11
ordinal: 12000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Connects the two previous tasks end to end for a single estate ID: fetch its detail, then persist/update the listing and append a snapshot if the payload changed. This is the smallest possible vertical slice through scraping -> listing and the base every later milestone (bulk collection, scheduling) builds on.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Fetching a not-yet-seen estate ID creates a new listing row and its first snapshot
- [ ] #2 Re-fetching the same estate ID with an unchanged payload updates last_seen_at but writes no new snapshot
- [ ] #3 Re-fetching the same estate ID with a changed payload updates the listing and writes a new snapshot
- [ ] #4 An integration test (WireMock fixture + Testcontainers Postgres) covers all three cases
<!-- AC:END -->
