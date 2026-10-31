---
id: TASK-28
title: Check Sreality detail payload for per-request volatile fields
status: Done
assignee:
  - '@lkrasa'
created_date: '2026-10-31T13:01:00Z'
updated_date: '2026-10-31T13:14:00Z'
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
- [x] #1 Two live captures of the same estate are compared and the differing fields (if any) are listed in doc-1
- [x] #2 If volatile fields exist, the change check ignores them while the stored snapshot payload stays the raw response, covered by a test
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Capture the same estate (ID 2494705740, already confirmed live from TASK-10) twice, ~5 minutes apart, via direct curl against GET /api/v1/estates/{id} (same headers the client uses).
2. Diff the two raw JSON payloads field-by-field (not just string diff, since key order/whitespace could differ even with identical semantic content - compare parsed JSON structurally).
3. If any fields differ and are clearly per-request-volatile (counters, timestamps, labels/labels_extended POI blocks, image URLs with tokens, etc.) rather than genuine listing changes, record them in doc-1.
4. If volatile fields exist: change ListingSnapshotService's hash computation to exclude them (e.g. parse to a JSON tree, strip the volatile paths, then hash the canonicalized remainder) while still storing the raw, unmodified payload string in listing_snapshot.payload, per decision-2. Cover with a test proving two captures differing only in the volatile fields are treated as unchanged (one snapshot row), while a listing-semantic change still produces a second snapshot row.
5. If no volatile fields are found: record that finding in doc-1 and leave ListingSnapshotService as-is (AC #2's 'if volatile fields exist' condition would not apply).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Two live captures of estate 2494705740, 5 minutes apart (14:07:15 and 14:12:38 CET on 2026-10-31), via GET /api/v1/estates/{id} with the same headers SrealityClient uses, were byte-for-byte identical (matching SHA-256 digests). No volatile fields found. AC #2 is vacuously satisfied: its precondition ('if volatile fields exist') did not hold, so ListingSnapshotService's whole-payload-hash approach needs no change and no new test — there is nothing to exclude from the hash. Finding recorded in doc-1 under a new 'Per-request volatility — checked, none found' subsection, including the caveat that this was only checked on one estate over one 5-minute gap.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Captured estate 2494705740 twice via GET /api/v1/estates/{id}, 5 minutes apart, using the same headers SrealityClient sends. The two raw responses were byte-for-byte identical (matching SHA-256 digests) - no per-request volatile fields (view counters, timestamps, token-bearing image URLs, recomputed labels/labels_extended POI blocks, etc.) were found. Recorded this in doc-1 (new 'Per-request volatility - checked, none found' subsection), with the caveat that it was only checked on one estate over one 5-minute gap. AC #2 is vacuously satisfied since its 'if volatile fields exist' precondition didn't hold - ListingSnapshotService (TASK-11) needs no change.
<!-- SECTION:FINAL_SUMMARY:END -->
