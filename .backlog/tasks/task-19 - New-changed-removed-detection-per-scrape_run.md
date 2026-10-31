---
id: TASK-19
title: New/changed/removed detection per scrape_run
status: To Do
assignee: []
created_date: '2026-10-30T23:43:00Z'
labels:
  - listing
milestone: m-4
dependencies:
  - TASK-18
ordinal: 19000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Price/payload changes are already captured as new snapshots by M2's persistence logic; what's still missing is marking listings that stopped appearing in a run as removed, and summarising what changed in that run's scrape_run record. Scoped narrowly to removal detection and the run summary -- not a re-implementation of snapshot-level change detection.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 After a scheduled run completes, any previously-active listing not seen in that run has removed_at set
- [ ] #2 A listing that reappears in a later run after being marked removed has removed_at cleared
- [ ] #3 The owning scrape_run record is updated with counts of new, updated, and removed listings for that run
- [ ] #4 A Testcontainers-backed test covers a listing being removed in one run and reappearing in a subsequent run
<!-- AC:END -->
