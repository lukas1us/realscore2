---
id: TASK-23.3
title: List listings with recent price drops
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
ordinal: 41000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A price drop on an active listing is a sign of a motivated seller and the first thing worth checking when looking for a deal. Finding them should not require going through listings one by one.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An endpoint lists active listings whose price dropped within a given number of days (default configurable), with the old price, new price, percent drop and drop date
- [ ] #2 It can be filtered by sale or rent
- [ ] #3 Results are ordered by percent drop, largest first
- [ ] #4 README documents the endpoint and its parameters
<!-- AC:END -->
