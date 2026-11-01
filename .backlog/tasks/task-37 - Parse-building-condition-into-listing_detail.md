---
id: TASK-37
title: Parse building condition into listing_detail
status: To Do
assignee: []
created_date: '2026-11-01T17:07:00Z'
labels:
  - parsing
milestone: m-6
dependencies: []
type: feature
ordinal: 47000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Building condition (building_condition, e.g. Novostavba, Po rekonstrukci, Před rekonstrukcí) is one of the strongest value signals in the Sreality detail and is present on every sale flat sampled on 2026-11-01, but it is not parsed. Quality scoring (TASK-35) needs it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 listing_detail has the building condition code and label, added by a new Flyway migration
- [ ] #2 The parser fills them from building_condition; a missing or not-entered (value 0) condition is null
- [ ] #3 Existing rows are filled by running the reparse endpoint once, as documented in README
- [ ] #4 Extractor tests cover a known condition and a missing one
<!-- AC:END -->
