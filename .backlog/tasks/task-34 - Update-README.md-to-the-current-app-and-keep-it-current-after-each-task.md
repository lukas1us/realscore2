---
id: TASK-34
title: Update README.md to the current app and keep it current after each task
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T13:02:00Z'
updated_date: '2026-11-01T13:11:00Z'
labels:
  - docs
dependencies: []
references:
  - README.md
  - CLAUDE.md
  - .claude/skills/finish-task/SKILL.md
type: docs
ordinal: 38000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
README.md still describes the placeholder scaffold that M1 removed: "service for scoring properties", tests on in-memory H2, and /api/properties endpoints that no longer exist. Since then the app gained the Sreality scraping pipeline, raw snapshots, parsing, a scheduled scrape job and market benchmarks (commits for TASK-10 through TASK-33 and TASK-22), none of it documented for someone running the app. Nothing in the workflow prompts a README update, so it drifted. Decided approach for keeping it current: add a README check step to the existing finish-task skill (it already runs at the end of every task) and one line to the CLAUDE.md "task is done" working rule; a separate skill was rejected because it would only run when someone remembers to invoke it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 README.md describes what the app does now (collect Sreality listings, store raw snapshots, parse, market benchmarks) and drops the removed /api/properties endpoints and H2 references
- [x] #2 README.md lists every current HTTP endpoint (reparse, sale-price and rent benchmarks, actuator health) with a one-line description
- [x] #3 README.md documents the configuration env vars (DB_*, SCRAPE_CRON, SCRAPE_PRICE_CAP, SCRAPE_RENT_PRICE_CAP, SCRAPE_REFETCH_AFTER) and the realscore.analysis.* settings with defaults
- [x] #4 README.md states test requirements correctly (Testcontainers needs Docker; mvn verify) and the post-deploy reparse step
- [x] #5 The finish-task skill has a step that checks whether the change affects endpoints, configuration, run commands or behaviour described in README.md, and updates README.md in the same task if so
- [x] #6 CLAUDE.md working rules say a task is not done while README.md is out of date with its changes
- [x] #7 CLAUDE.md no longer describes the removed placeholder scaffold or H2: Commands examples use existing test classes, the test setup describes Testcontainers, and "Current state" matches the code
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Rewrite README.md: what it does (Sreality -> raw JSONB snapshots -> parsed listing_detail -> benchmarks), modules and data flow in a few lines, requirements (Java 25, Maven, Docker for Postgres and Testcontainers), running (compose / host), configuration table (DB_*, SCRAPE_CRON, SCRAPE_PRICE_CAP, SCRAPE_RENT_PRICE_CAP, SCRAPE_REFETCH_AFTER, realscore.analysis.large-cities / min-sample-size; defaults from application.properties), HTTP API (reparse, two benchmark GETs, /actuator/health), operations notes (reparse after parser/schema changes, first run with rentals is long, 5M sale cap bias), testing (mvn verify, Docker needed). Drop /api/properties and H2.
2. finish-task skill: new step after the scope check - if the change adds/changes endpoints, config/env vars, run commands or documented behaviour, update README.md in this task and include it in the commit list.
3. CLAUDE.md "Working rules": the done definition gains "README.md reflects the change".
4. Verify every endpoint/property in README against code and application.properties; mvn verify.

5. (scope added by user) CLAUDE.md: fix Commands test examples, replace H2 paragraph with Testcontainers, rewrite "Current state" to the actual module layout.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
README endpoints checked against the @GetMapping/@PostMapping annotations; config defaults checked against application.properties and docker-compose.yml. README check added to finish-task as step 5 (later steps renumbered). CLAUDE.md fix (H2, placeholder scaffold, stale test examples) added to scope at the user's request.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
README.md rewritten to describe the current app: pipeline (scrape -> raw snapshot -> parse -> benchmarks), modules, requirements (Docker for Testcontainers), running, configuration table with defaults, HTTP API (benchmarks, reparse, health), operations notes (reparse after parser changes, long first run, sale cap bias) and testing; removed /api/properties and H2. To keep it current, the finish-task skill now checks README.md against each change, and the CLAUDE.md done rule requires README.md to reflect it. CLAUDE.md also fixed: test examples use MarketBenchmarkServiceTest, Testcontainers replaces the H2 note, and "Current state" describes the real modules. mvn verify passes (docs only).
<!-- SECTION:FINAL_SUMMARY:END -->
