---
id: TASK-13
title: Region/disposition result-size prober
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
ordinal: 13000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The bucketing workaround for Sreality's ~1000-result cap needs a cheap way to ask 'how many results would this query match' before deciding whether to subdivide. See the 'Sreality API' doc's cap/bucketing section for the per_page=1 probe technique this is based on.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A function probes GET /api/cs/v2/estates with per_page=1, page=1 for a given set of search params and returns the parsed result_size
- [ ] #2 A WireMock test using a fixture response asserts result_size is parsed correctly
<!-- AC:END -->
