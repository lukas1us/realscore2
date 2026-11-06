---
id: TASK-50
title: Record parse failures and missing key fields per scrape run
status: To Do
assignee: []
created_date: '2026-11-06T13:55:00Z'
labels: []
dependencies: []
priority: medium
type: enhancement
ordinal: 63000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When parsing a new snapshot fails, `SnapshotRecordedListener` logs it and moves on, and nothing records it. The Sreality API is undocumented and has changed its whole shape once without notice (doc "Sreality API", biggest open risks). A new change would show up only as missing listing_detail rows or fields that suddenly go NULL, and would be noticed late, through odd medians or scores. Each run should make such drift visible.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Each scrape_run records how many snapshots of that run failed to parse
- [ ] #2 Each scrape_run records, or logs as a warning, how many listings parsed in that run lack price, size or disposition
- [ ] #3 A parse failure still does not fail the run or lose the stored snapshot
- [ ] #4 Tests cover a run with a failing parse and a run with missing key fields
- [ ] #5 README.md describes the new counts
<!-- AC:END -->
