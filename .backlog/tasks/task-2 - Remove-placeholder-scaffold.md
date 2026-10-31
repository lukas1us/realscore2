---
id: TASK-2
title: Remove placeholder scaffold
status: To Do
assignee: []
created_date: '2026-10-30T23:42:00Z'
labels:
  - infra
milestone: m-1
dependencies: []
ordinal: 2000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The current model/repository/controller/service scaffold (Property, PropertyRepository, PropertyController, PropertyScoringService) is a US-style price-per-sqft/beds/baths placeholder explicitly called out in CLAUDE.md as not to be built on. It has no place in the target domain (Czech real estate listings scraped from portals) and should be cleared out before the real package structure is introduced, so nothing is ever tempted to extend it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 model/, repository/, controller/ packages and PropertyScoringService (plus their tests) no longer exist
- [ ] #2 mvn test passes with no references to the removed classes anywhere in src
<!-- AC:END -->
