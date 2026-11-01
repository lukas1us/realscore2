---
id: TASK-23
title: 'Later: price history over time per listing'
status: Done
assignee: []
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-11-01T16:10:00Z'
labels:
  - analysis
milestone: m-6
dependencies: []
ordinal: 23000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Coarse placeholder for surfacing the price-change history that listing_snapshot makes possible (multiple snapshots per listing over time). Not split into subtasks yet.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Scope and subtasks are defined when this task is started
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Scoped into TASK-23.1 (listing_price_point history, filled on parse and by reparse), TASK-23.2 (GET /api/listings/{source}/{externalId} with history and stats) and TASK-23.3 (GET /api/analysis/price-drops). An earlier draft proposal from another session (listing_price_observation table, /api/analysis/... endpoints) was superseded by these subtasks.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
All subtasks done: price history per listing (23.1), single listing endpoint with history and stats (23.2), recent price drops endpoint (23.3).
<!-- SECTION:FINAL_SUMMARY:END -->
