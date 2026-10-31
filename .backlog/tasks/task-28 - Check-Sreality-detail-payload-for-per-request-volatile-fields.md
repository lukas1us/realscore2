---
id: TASK-28
title: Check Sreality detail payload for per-request volatile fields
status: To Do
assignee: []
created_date: '2026-10-31T13:01:00Z'
labels:
  - scraping
  - listing
milestone: m-2
dependencies:
  - TASK-12
ordinal: 28000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ListingSnapshotService (TASK-11) decides 'payload changed' by SHA-256 of the raw response string. If live Sreality v1 detail responses contain fields that change on every request (e.g. view counters, timestamps, the mapy.cz 'labels'/'labels_extended' POI blocks, image URLs with tokens), every fetch writes a new snapshot and the dedup is defeated. TASK-12's tests use a fixed fixture and cannot reveal this. Capture the same estate twice via /capture-fixture (a few minutes apart) and diff them; if volatile fields exist, decide how to exclude them from the hash (raw payload must still be stored unchanged, per decision-2) and record the finding in doc-1.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Two live captures of the same estate are compared and the differing fields (if any) are listed in doc-1
- [ ] #2 If volatile fields exist, the change check ignores them while the stored snapshot payload stays the raw response, covered by a test
<!-- AC:END -->
