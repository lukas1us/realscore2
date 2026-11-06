---
id: TASK-49
title: Parse GPS coordinates and street into listing_detail
status: To Do
assignee: []
created_date: '2026-11-06T13:55:00Z'
labels: []
dependencies: []
priority: medium
type: feature
ordinal: 62000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Every Sreality detail payload has `locality.gps_lat`, `locality.gps_lon` and `locality.street` (doc "Sreality API", fixture `detail_success.json`), but parsing drops them. TASK-25.2 needs a map of active listings from GPS, and finding duplicate listings of the same flat needs location. The data is already in the stored snapshots, so a reparse fills existing rows without rescraping. `locality.zip` was -1 in the fixture, so it is not reliable.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 listing_detail holds latitude, longitude and street for each listing whose payload has them, and NULL otherwise
- [ ] #2 Existing listings get the values by a reparse; raw snapshots are not changed
- [ ] #3 The listing endpoint exposes the new fields
- [ ] #4 Tests cover a payload with and without coordinates and street
- [ ] #5 README.md documents the new fields and the reparse needed after upgrading
<!-- AC:END -->
