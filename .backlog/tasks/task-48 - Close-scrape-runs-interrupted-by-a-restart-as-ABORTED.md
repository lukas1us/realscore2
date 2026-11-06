---
id: TASK-48
title: Close scrape runs interrupted by a restart as ABORTED
status: To Do
assignee: []
created_date: '2026-11-06T13:55:00Z'
labels: []
dependencies: []
priority: medium
type: bug
ordinal: 61000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A `scrape_run` row is opened as RUNNING and only closed by `SrealityScrapeJob.run()` itself. If the app is stopped or redeployed mid-run (the first run after adding a search can take hours), the row stays RUNNING forever, its counts stay partial, and the removal check of that run never happens. `ScrapeRunStatus` has no state for this, although TASK-25.2 already expects ABORTED runs on the scrape-run dashboard. Related but separate: TASK-44 covers runs that never start while the host sleeps.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A run that was RUNNING when the app stopped is shown as ABORTED once the app is running again, with a finished time and a reason in error details
- [ ] #2 A run that is actually in progress is never marked ABORTED
- [ ] #3 Listings are not marked removed on behalf of an aborted run
- [ ] #4 Tests cover an interrupted run and a run in progress
- [ ] #5 README.md lists the ABORTED status and when it is set
<!-- AC:END -->
