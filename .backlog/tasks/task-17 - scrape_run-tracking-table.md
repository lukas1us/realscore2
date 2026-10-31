---
id: TASK-17
title: scrape_run tracking table
status: Done
assignee:
  - '@lkrasa'
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-10-31T13:02:00Z'
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
- [x] #1 A Flyway migration creates scrape_run (status, started_at, finished_at, found/new/updated/removed/error counts, error details)
- [x] #2 A repository/service can open a run, update its counts, and close it with a final status
- [x] #3 A Testcontainers-backed test exercises opening, updating, and closing a run
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add V3__create_scrape_run.sql (reviewed and approved) creating scrape_run.
2. Add a public ScrapeRunStatus enum (com.realscore.scraping) and an internal ScrapeRun JPA entity + ScrapeRunRepository (com.realscore.scraping.internal).
3. Add public ScrapeRunService (com.realscore.scraping) with openRun() -> run id, updateCounts(runId, found, new, updated, removed, errors), and closeRun(runId, status, errorDetails).
4. Add a Testcontainers-backed test exercising open -> update counts -> close, asserting the persisted row via the repository.
5. Run mvn verify to confirm everything passes.
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added V3__create_scrape_run.sql (status, started_at, finished_at, found/new/updated/removed/error counts, error_details). Added public ScrapeRunStatus enum (RUNNING/SUCCEEDED/FAILED) and internal ScrapeRun entity + ScrapeRunRepository (com.realscore.scraping.internal), plus a public ScrapeRunService with openRun() -> run id, updateCounts(runId, found, new, updated, removed, errors), and closeRun(runId, status, errorDetails). ScrapeRunServiceTest (Testcontainers Postgres) proves the open -> update -> close lifecycle and that closing with FAILED records error details. mvn verify passes (13 tests total across the whole app, including ModularityTests with no boundary violations against the concurrently in-progress TASK-12 work).
<!-- SECTION:FINAL_SUMMARY:END -->
