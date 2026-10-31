---
id: TASK-6
title: Add Testcontainers Postgres test infrastructure
status: To Do
assignee: []
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-30T23:42:00Z'
labels:
  - infra
milestone: m-1
dependencies:
  - TASK-5
ordinal: 6000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Per the 'DB tests use Testcontainers Postgres' decision, H2 cannot represent the JSONB-backed schema the target architecture needs (listing_snapshot). A shared base/config for Postgres-backed tests needs to exist before M2 writes the first real persistence test.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A reusable test base or configuration starts a Postgres Testcontainer and runs Flyway migrations against it
- [ ] #2 One smoke test using it passes, proving the container-backed context loads successfully
- [ ] #3 The H2 test dependency and its application.properties override are removed
<!-- AC:END -->
