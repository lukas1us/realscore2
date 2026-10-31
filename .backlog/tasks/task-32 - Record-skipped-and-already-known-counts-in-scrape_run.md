---
id: TASK-32
title: Record skipped and already-known counts in scrape_run
status: To Do
assignee: []
created_date: '2026-10-31T22:34:00Z'
labels:
  - scraping
dependencies:
  - TASK-18
ordinal: 32000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SrealityBulkCollector (TASK-16) returns BulkCollectionResult(found, alreadyKnown, fetched, skipped, failed), but scrape_run (V3 migration, TASK-17) only has found/new/updated/removed/error columns, so SrealityScrapeJob (TASK-18) stores found, new=fetched and errors=failed and the skipped (HTTP 403/404) and already-known counts appear only in the log. Add columns for them through a new Flyway migration (never edit V3), map them in ScrapeRun/ScrapeRunService, and have the job record them.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A new Flyway migration adds skipped and already-known count columns to scrape_run without editing existing migrations
- [ ] #2 SrealityScrapeJob records the skipped and already-known counts of each run
- [ ] #3 A Testcontainers-backed test asserts both counts are stored for a run that has skipped and already-known IDs
<!-- AC:END -->
