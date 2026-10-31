---
id: TASK-17
title: scrape_run tracking table
status: To Do
assignee: []
created_date: '2026-10-30T23:43:00Z'
labels:
  - scraping
milestone: m-4
dependencies:
  - TASK-9
ordinal: 17000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implements the 'every scrape job execution is recorded in scrape_run' decision as an actual table and repository, ahead of anything that would write to it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A Flyway migration creates scrape_run (status, started_at, finished_at, found/new/updated/removed/error counts, error details)
- [ ] #2 A repository/service can open a run, update its counts, and close it with a final status
- [ ] #3 A Testcontainers-backed test exercises opening, updating, and closing a run
<!-- AC:END -->
