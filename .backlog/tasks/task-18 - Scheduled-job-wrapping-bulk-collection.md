---
id: TASK-18
title: Scheduled job wrapping bulk collection
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-10-31T22:44:00Z'
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
- [x] #1 A @Scheduled job triggers the M3 bulk pipeline on a cron expression read from application configuration
- [x] #2 Each execution opens a scrape_run at start and closes it with final counts/status at the end, including on failure
- [x] #3 A test triggers the job method directly (not by waiting on the real cron) and asserts a scrape_run is created and closed correctly
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. ScrapeJobProperties (@ConfigurationProperties realscore.scrape, validated): cron, priceCap; registered with @EnableScheduling on SrealityConfiguration.
2. SrealityScrapeJob.run(): @Scheduled(cron = ${realscore.scrape.cron}, zone Europe/Prague); openRun -> bulkCollector.collect(priceCap) -> updateCounts(found, fetched, 0, 0, failed) -> closeRun SUCCEEDED; any exception -> closeRun FAILED with class + message, logged, not rethrown.
3. application.properties: realscore.scrape.cron=${SCRAPE_CRON:-} (disabled by default), price-cap=${SCRAPE_PRICE_CAP:5000000}; docker-compose app gets SCRAPE_CRON '0 0 3 * * *'.
4. SrealityScrapeJobTest (SpringBootTest + Testcontainers + WireMock): success run counts, failed run on search 500, ScheduledTaskHolder has a cron task with the configured expression.
Follow-up: skipped/already-known counts in scrape_run (new task, user approved).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented as planned. SrealityScrapeJob.run() is @Scheduled(cron = ${realscore.scrape.cron}, zone = Europe/Prague); opens a scrape_run, runs SrealityBulkCollector.collect(priceCap), stores found / new=fetched / updated=0 / removed=0 / errors=failed and closes SUCCEEDED; any RuntimeException closes FAILED with '<ExceptionClass>: <message>' and is logged, not rethrown. ScrapeJobProperties (cron @NotBlank, priceCap @Positive) and @EnableScheduling on SrealityConfiguration. application.properties: realscore.scrape.cron=${SCRAPE_CRON:-} (disabled unless set, so tests and spring-boot:run never scrape the live site), price-cap=${SCRAPE_PRICE_CAP:5000000}; docker-compose.yml app service sets SCRAPE_CRON '0 0 3 * * *' (user approved defaults). Tests: SrealityScrapeJobTest (3): cron task registered with the configured expression via ScheduledTaskHolder, successful run counts, failed run on search 500. Follow-up TASK-32 (skipped/already-known counts in scrape_run), user approved. mvn verify: 53 tests, BUILD SUCCESS.

Finalized via /finish-task: mvn verify green (53 tests). AC evidence: #1 SrealityScrapeJob @Scheduled(cron = ${realscore.scrape.cron}) + SrealityScrapeJobTest.jobIsScheduledOnTheConfiguredCron; #2 successfulRunIsClosedWithItsCounts and failedRunIsClosedAsFailedWithTheCause; #3 both call run() directly. Follow-up: TASK-32.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added the scheduled scrape: SrealityScrapeJob runs the M3 bulk collection on realscore.scrape.cron (Europe/Prague) with realscore.scrape.price-cap (default 5,000,000 CZK), and records every execution in scrape_run: opened at start, closed SUCCEEDED with found/new/error counts, or FAILED with the exception class and message, never left RUNNING after a failure inside the run. Scheduling is off unless SCRAPE_CRON is set; docker compose sets it to daily 03:00, so tests and local runs never hit the live API. Tests call run() directly against WireMock and Postgres (success and failure) and check the cron task is registered with the configured expression. mvn verify green, 53 tests. Follow-up: TASK-32 for skipped/already-known counts; updated/removed counts come with TASK-19/TASK-31.
<!-- SECTION:FINAL_SUMMARY:END -->
