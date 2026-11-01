---
id: TASK-23.2
title: Single listing endpoint with price history
status: To Do
assignee: []
created_date: '2026-11-01T14:16:00Z'
labels:
  - analysis
milestone: m-6
dependencies:
  - TASK-23.1
parent_task_id: TASK-23
type: feature
ordinal: 40000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
There is no way to look at one listing through the app; today it needs SQL against Postgres. Seeing a listing together with its price history is the basic view needed before scoring or a UI.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 GET /api/listings/{source}/{externalId} returns the parsed details, first seen, last seen, removed time (if any) and the price history ordered by time
- [ ] #2 The response includes days on market, number of price drops, and percent change from the first to the current price
- [ ] #3 An unknown listing returns 404
- [ ] #4 README documents the endpoint
<!-- AC:END -->
