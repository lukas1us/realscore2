---
id: TASK-18
title: Scheduled job wrapping bulk collection
status: To Do
assignee: []
created_date: '2026-10-30T23:43:00Z'
labels:
  - scraping
milestone: m-4
dependencies:
  - TASK-16
  - TASK-17
ordinal: 18000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Per the 'run everything via docker compose, scraping is a scheduled job' decision: wraps the M3 bulk fetch+persist pipeline in a @Scheduled job whose cron expression comes from configuration, recording each execution as a scrape_run.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A @Scheduled job triggers the M3 bulk pipeline on a cron expression read from application configuration
- [ ] #2 Each execution opens a scrape_run at start and closes it with final counts/status at the end, including on failure
- [ ] #3 A test triggers the job method directly (not by waiting on the real cron) and asserts a scrape_run is created and closed correctly
<!-- AC:END -->
