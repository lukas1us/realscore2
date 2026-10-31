---
id: TASK-10
title: Sreality client fetches one estate detail
status: To Do
assignee: []
created_date: '2026-10-30T23:42:00Z'
labels:
  - scraping
milestone: m-2
dependencies:
  - TASK-9
documentation:
  - doc-1
ordinal: 10000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The first real feature slice: talk to Sreality's detail endpoint and get raw JSON back, with nothing else attached yet (no persistence, no bulk collection). Keeping this isolated makes it easy to verify the HTTP/parsing boundary in isolation before building anything on top of it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A client in com.realscore.scraping calls GET https://www.sreality.cz/api/cs/v2/estates/{id} and returns the raw response body/JSON
- [ ] #2 A WireMock test using a fixture JSON under src/test/resources/fixtures verifies a successful fetch
- [ ] #3 A non-200 response (e.g. 404) raises a typed exception rather than propagating a generic HTTP client error
<!-- AC:END -->
