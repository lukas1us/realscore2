---
id: TASK-55
title: Add more quality factors from fields already in the snapshots
status: To Do
assignee: []
created_date: '2026-11-06T13:55:00Z'
labels: []
dependencies:
  - TASK-46
priority: low
type: feature
ordinal: 68000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The quality part of the score (TASK-35) uses ownership, construction, energy class, building condition and elevator. The stored v1 payloads also carry balcony, loggia, terrace, cellar, parking and garage flags, the building's total `floors` (top or ground floor), `furnished`, `heating_set` and `flat_class` (fixture `detail_success.json`), which buyers weigh too. These need only parsing and a reparse, not a rescrape. Floor-based factors depend on the floor convention fixed in TASK-46. `flat_class` was "- vyber typ" (0, not entered) in the fixture, so not-entered values must count as unknown, as in TASK-36.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The chosen new fields are parsed into listing_detail, with not-entered values stored as NULL, and existing rows are filled by a reparse
- [ ] #2 Each new factor has configurable points, adds 0 when unknown, and appears in the score factors list
- [ ] #3 Top-floor and ground-floor factors use the floor convention from TASK-46
- [ ] #4 Tests cover each new factor with known, not-entered and missing values
- [ ] #5 README.md and application.properties document the new fields and points
<!-- AC:END -->
