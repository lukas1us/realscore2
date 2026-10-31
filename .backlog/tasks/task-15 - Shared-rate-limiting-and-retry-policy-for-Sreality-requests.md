---
id: TASK-15
title: Shared rate limiting and retry policy for Sreality requests
status: To Do
assignee: []
created_date: '2026-10-30T23:43:00Z'
labels:
  - scraping
milestone: m-3
dependencies:
  - TASK-10
documentation:
  - doc-1
ordinal: 15000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Centralises the request pacing and failure handling the old app scattered across its scan jobs (see the 'Sreality API' doc's rate limiting section) into one policy used by every Sreality call, instead of each caller reimplementing its own backoff/skip rules.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 HTTP 429 responses are retried with exponential backoff up to a configurable max attempts
- [ ] #2 HTTP 403 and 404 responses are not retried and are treated as a skip, not an error
- [ ] #3 A configurable minimum delay is applied between requests
- [ ] #4 Unit tests cover the 429-retry, 403/404-skip, and transient-error-retry branches independently
<!-- AC:END -->
