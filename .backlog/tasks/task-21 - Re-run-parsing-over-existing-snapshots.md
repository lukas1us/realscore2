---
id: TASK-21
title: Re-run parsing over existing snapshots
status: To Do
assignee: []
created_date: '2026-10-30T23:43:00Z'
labels:
  - parsing
milestone: m-5
dependencies:
  - TASK-20
ordinal: 21000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Normalisation logic will keep changing as new edge cases are found (as happened repeatedly in the old app's fix: commits); this task makes it possible to re-derive listing details for all already-stored snapshots whenever the parser changes, without any new scraping.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A command/job re-parses every stored snapshot (or a specified subset) and overwrites the corresponding listing-details rows
- [ ] #2 Running it twice in a row with no parser changes in between produces identical listing-details rows
<!-- AC:END -->
