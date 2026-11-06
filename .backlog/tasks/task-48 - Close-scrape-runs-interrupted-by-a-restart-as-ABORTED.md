---
id: TASK-48
title: Close scrape runs interrupted by a restart as ABORTED
status: Done
assignee:
  - '@claude'
created_date: '2026-11-06T13:55:00Z'
updated_date: '2026-11-06T19:59:00Z'
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
- [x] #1 A run that was RUNNING when the app stopped is shown as ABORTED once the app is running again, with a finished time and a reason in error details
- [x] #2 A run that is actually in progress is never marked ABORTED
- [x] #3 Listings are not marked removed on behalf of an aborted run
- [x] #4 Tests cover an interrupted run and a run in progress
- [x] #5 README.md lists the ABORTED status and when it is set
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add ABORTED to ScrapeRunStatus (status is VARCHAR(20), no constraint, no migration).
2. ScrapeRunService.abortInterruptedRuns(Instant appStartedAt): closes every RUNNING run started before appStartedAt as ABORTED with finished_at and a reason in error_details.
3. scraping/internal listener on ApplicationReadyEvent calls it with the app start time, so runs started by this instance are never touched.
4. Aborted runs never reach the removal check, so nothing is marked removed on their behalf; assert in tests.
5. Tests: interrupted run aborted, run started after app start stays RUNNING, no listing marked removed. README: status list and when ABORTED is set.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Added ScrapeRunStatus.ABORTED (status is VARCHAR(20) without a constraint, so no migration). ScrapeRunService.abortInterruptedRuns(startedBefore) bulk-closes RUNNING runs started before the given instant as ABORTED with finished_at = now and ScrapeRunService.ABORTED_REASON in error_details. InterruptedRunCleanup (scraping/internal) captures its creation instant during context startup, i.e. before the scheduler can open a run, and calls it on ApplicationReadyEvent, so a run opened by this app start is never aborted. Counts of an aborted run stay partial; the removal check is the last step of a run, so nothing is marked removed for it. Assumes one app instance per DB (a second instance started against the same DB would abort the first one's in-progress run).
Tests: InterruptedRunCleanupTest (interrupted run aborted with finished time and reason; run in progress stays RUNNING; FAILED run untouched; no listing marked removed). README: scrape_run statuses in How it works. mvn verify green (267 tests).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Runs left RUNNING by an app stop are closed as ABORTED (finished_at, reason in error_details) once the app is ready again. Only runs started before this app start are touched, so an in-progress run is never aborted; aborted runs mark nothing removed. Covered by InterruptedRunCleanupTest; README lists the statuses. Risk: assumes a single app instance per database.
<!-- SECTION:FINAL_SUMMARY:END -->
