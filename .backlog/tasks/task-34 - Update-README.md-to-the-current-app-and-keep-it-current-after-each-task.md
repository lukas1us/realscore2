---
id: TASK-34
title: Update README.md to the current app and keep it current after each task
status: To Do
assignee: []
created_date: '2026-11-01T13:02:00Z'
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
- [ ] #1 README.md describes what the app does now (collect Sreality listings, store raw snapshots, parse, market benchmarks) and drops the removed /api/properties endpoints and H2 references
- [ ] #2 README.md lists every current HTTP endpoint (reparse, sale-price and rent benchmarks, actuator health) with a one-line description
- [ ] #3 README.md documents the configuration env vars (DB_*, SCRAPE_CRON, SCRAPE_PRICE_CAP, SCRAPE_RENT_PRICE_CAP, SCRAPE_REFETCH_AFTER) and the realscore.analysis.* settings with defaults
- [ ] #4 README.md states test requirements correctly (Testcontainers needs Docker; mvn verify) and the post-deploy reparse step
- [ ] #5 The finish-task skill has a step that checks whether the change affects endpoints, configuration, run commands or behaviour described in README.md, and updates README.md in the same task if so
- [ ] #6 CLAUDE.md working rules say a task is not done while README.md is out of date with its changes
<!-- AC:END -->
