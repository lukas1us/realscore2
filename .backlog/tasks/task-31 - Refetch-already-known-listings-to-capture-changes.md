---
id: TASK-31
title: Refetch already-known listings to capture changes
status: To Do
assignee: []
created_date: '2026-10-31T21:32:00Z'
labels:
  - scraping
  - listing
dependencies:
  - TASK-16
ordinal: 31000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SrealityBulkCollector (TASK-16) fetches details only for collected IDs that have no listing yet; IDs that are already known are skipped entirely. As a result, price or other payload changes on known listings are never captured, and their last_seen_at is not updated by the bulk run, even though ListingSnapshotService.recordFetch already writes a new snapshot when the payload hash changes. Decide on and implement a refetch strategy for known listings (e.g. refetch every known collected ID each run, or only those not fetched within a configurable interval), keeping request volume within the shared Sreality rate-limit policy (TASK-15). Coordinate with TASK-19, which uses per-run 'seen' information for removal detection.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Known listings that appear in a bulk run's collected IDs are refetched according to a documented, configurable strategy
- [ ] #2 A refetched listing whose payload changed gets a new snapshot; an unchanged one only has last_seen_at updated
- [ ] #3 A Testcontainers-backed test covers a known listing being refetched with a changed and with an unchanged payload
<!-- AC:END -->
