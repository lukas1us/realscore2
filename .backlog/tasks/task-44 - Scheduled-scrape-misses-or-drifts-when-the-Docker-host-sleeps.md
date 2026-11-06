---
id: TASK-44
title: Scheduled scrape misses or drifts when the Docker host sleeps
status: Done
assignee:
  - '@claude'
created_date: '2026-11-05T11:48:00Z'
updated_date: '2026-11-06T20:43:00Z'
labels:
  - scraping
dependencies: []
priority: high
type: bug
ordinal: 57000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Observed 2026-11-05: the app container had been up ~27h wall-clock (since 2026-11-04 08:50 UTC) with `SCRAPE_CRON=0 0 3 * * *`, yet no scrape ran at 03:00 Prague on 2026-11-05 and `scrape_run` has no row since run 2. A thread dump showed the JVM uptime was only ~11.2h and `scheduling-1` had used 0.08 ms CPU, so it had never run the job. The Mac (Docker Desktop VM) sleeps, and Spring cron delays are waited out on the monotonic clock, which stops while suspended. A run therefore fires hours late at an arbitrary wall-clock time, or a day is skipped entirely, with nothing in the logs. The app is meant to collect history on a fixed cadence, so silent gaps break price history and removal detection.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A scrape that was due while the host was asleep or the app was down starts soon after the app is running again, not at the next cron slot
- [x] #2 Runs that were due several times during one long outage start only once
- [x] #3 No second run starts while one is already running
- [x] #4 The behaviour is covered by tests (time is simulated; nothing sleeps for real)
- [x] #5 README.md describes when scheduled runs happen and how missed runs are caught up
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. ScrapeSchedule (scraping/internal): wraps the cron (Europe/Prague) and a Clock; latestSlot(now) = last cron slot at or before now; isDue(latestNonAbortedStart, now): due when a slot has passed and no non-aborted run started at or after it.
2. Replace @Scheduled(cron) on SrealityScrapeJob.run() with a checker @Scheduled(fixedDelay = realscore.scrape.check-interval, default 1m) that reads the wall clock each time and calls run() when due. fixedDelay on the single scheduler thread prevents overlap; also skip while a run is RUNNING. '-' disables it.
3. ScrapeRunService: latest start of a run that is not ABORTED, and whether a run is RUNNING.
4. Clock bean for scraping; tests use a fixed/mutable Clock, nothing sleeps.
5. Tests: missed slot runs on next check; several missed slots run once; not due before next slot; run in progress skipped; disabled cron; aborted run is retried. Update SrealityScrapeJobTest's schedule test.
6. README: when scheduled runs happen and how missed runs are caught up; new SCRAPE_CHECK_INTERVAL property.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Replaced @Scheduled(cron) on SrealityScrapeJob.run() with ScrapeScheduler.runIfDue(), a @Scheduled(fixedDelay = realscore.scrape.check-interval, default 1m / SCRAPE_CHECK_INTERVAL) check that reads the wall clock (injected Clock). ScrapeSchedule wraps the CronExpression in Europe/Prague. A run is due when the first slot after the latest non-ABORTED run's start (ScrapeRunService.findLatestStartNotAborted) is <= now; with no run ever, slots count from the app start, so a fresh install or test context waits for the next slot. One run catches up any number of missed slots. Overlap: checks are sequential on the scheduler thread and wait for the run; SrealityScrapeJob.run() also has an AtomicBoolean guard. ScrapeScheduler is built in SrealityConfiguration (like SrealityRequestPolicy) so no app-wide Clock bean is added.
Trade-off: runs now start up to check-interval after their slot instead of exactly on it.
Tests: ScrapeSchedulerTest (FakeTime: slot missed while asleep, several missed slots -> one run, next slot after catch-up, not due before slot, run in progress, no runs yet, disabled), SrealityScrapeJobOverlapTest (re-entrant start opens no second run), ScrapeRunServiceTest.latestStartLeavesOutAbortedRuns, SrealityScrapeJobTest now asserts the fixed-delay check. README: SCRAPE_CHECK_INTERVAL row and an Operations note on scheduling/catch-up. mvn verify green (276 tests).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Scheduled scrapes no longer depend on Spring's monotonic cron delays. A check every SCRAPE_CHECK_INTERVAL (1m) compares the wall clock with SCRAPE_CRON and starts a run once a slot has passed since the latest non-aborted run, so a slot missed while the host slept or the app was down runs right after wake/restart, once. Overlapping runs are prevented. Covered by simulated-time unit tests; README documents it. Runs now start up to one check interval after their slot.
<!-- SECTION:FINAL_SUMMARY:END -->
