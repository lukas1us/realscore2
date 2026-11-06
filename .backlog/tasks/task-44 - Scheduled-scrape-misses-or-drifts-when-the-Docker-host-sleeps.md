---
id: TASK-44
title: Scheduled scrape misses or drifts when the Docker host sleeps
status: To Do
assignee: []
created_date: '2026-11-05T11:48:00Z'
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
- [ ] #1 A scrape that was due while the host was asleep or the app was down starts soon after the app is running again, not at the next cron slot
- [ ] #2 Runs that were due several times during one long outage start only once
- [ ] #3 No second run starts while one is already running
- [ ] #4 The behaviour is covered by tests (time is simulated; nothing sleeps for real)
- [ ] #5 README.md describes when scheduled runs happen and how missed runs are caught up
<!-- AC:END -->
