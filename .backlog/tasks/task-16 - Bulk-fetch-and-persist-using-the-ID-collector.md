---
id: TASK-16
title: Bulk fetch and persist using the ID collector
status: To Do
assignee: []
created_date: '2026-10-30T23:43:00Z'
labels:
  - scraping
  - listing
milestone: m-3
dependencies:
  - TASK-14
  - TASK-15
  - TASK-12
ordinal: 16000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The end-to-end bulk path: run the ID collector for a price-capped sale-apartment search, fetch each newly-seen ID's detail through the rate-limited client, and persist each via the same listing+snapshot pipeline built for a single listing in M2. This is the piece M4's scheduled job will later wrap.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Running the pipeline for a given price cap persists a listing+snapshot for every collected ID not already known
- [ ] #2 An ID that fails with a non-retriable error (403/404) is skipped without aborting the rest of the run
- [ ] #3 An integration test using a handful of fixture IDs, including one 404 and one 429-then-success, verifies the expected listings are persisted and the failures are handled as specified
<!-- AC:END -->
