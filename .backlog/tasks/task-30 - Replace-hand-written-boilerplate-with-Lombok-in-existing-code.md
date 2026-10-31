---
id: TASK-30
title: Replace hand-written boilerplate with Lombok in existing code
status: To Do
assignee: []
created_date: '2026-10-31T13:35:00Z'
labels:
  - infra
dependencies: []
ordinal: 30000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
CLAUDE.md now requires Lombok for boilerplate (getters/setters, constructors, builders). Code written before that rule still hand-writes it, e.g. the dependency-injection constructors of SrealityEstateFetcher (scraping.internal) and ListingSnapshotService (listing), and any other hand-written accessors/constructors in main code. Convert them (e.g. @RequiredArgsConstructor, keeping current constructor visibility where it matters for module boundaries) without changing behaviour. SrealityApiException was already converted in TASK-13; constructors that contain logic (not just field assignment) stay hand-written.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Main code has no hand-written constructors/accessors that only assign or return fields; Lombok is used instead
- [ ] #2 Behaviour and public API names are unchanged; mvn verify passes
<!-- AC:END -->
