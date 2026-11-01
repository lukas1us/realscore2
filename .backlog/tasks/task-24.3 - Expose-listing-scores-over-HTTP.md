---
id: TASK-24.3
title: Expose listing scores over HTTP
status: To Do
assignee: []
created_date: '2026-11-01T16:20:00Z'
labels:
  - analysis
milestone: m-6
dependencies:
  - TASK-24.2
parent_task_id: TASK-24
type: feature
ordinal: 44000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Scores (TASK-24.2) are only useful once they can be browsed: a ranked list to find the best deals, and the score shown when looking at one listing.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 GET /api/analysis/scores returns scored listings best first, limited by a limit parameter with a configurable default
- [ ] #2 The list can be filtered by region, city and disposition
- [ ] #3 GET /api/listings/{source}/{externalId} includes the score breakdown for an active sale flat that can be scored, and null otherwise
- [ ] #4 Invalid parameters return 400
- [ ] #5 README documents the endpoint, its parameters and the scoring configuration
<!-- AC:END -->
