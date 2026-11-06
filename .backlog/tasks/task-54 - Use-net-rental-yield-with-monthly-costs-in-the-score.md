---
id: TASK-54
title: Use net rental yield with monthly costs in the score
status: To Do
assignee: []
created_date: '2026-11-06T13:55:00Z'
labels: []
dependencies: []
priority: low
type: enhancement
ordinal: 67000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The rental-yield part is gross: expected rent × 12 / price. Monthly costs paid by the owner (fond oprav, service charges) differ a lot between buildings and change the real return. The v1 detail payload has a `cost_of_living` field, but it is a free-text string (empty in the fixture `detail_success.json`), so its content and reliability must first be checked on real snapshots and recorded in the Backlog doc "Sreality API".
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The meaning, format and fill rate of cost_of_living in real snapshots are recorded in the Backlog doc "Sreality API"
- [ ] #2 If usable, monthly costs are parsed into listing_detail (NULL when missing or not parseable) and the existing rows are filled by a reparse
- [ ] #3 The rental-yield part uses net yield when costs are known and gross yield otherwise, and the response says which one was used
- [ ] #4 Tests cover known, missing and unparseable costs
- [ ] #5 README.md documents the yield calculation
<!-- AC:END -->
