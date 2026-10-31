---
id: TASK-5
title: Add Flyway and switch ddl-auto to validate
status: To Do
assignee: []
created_date: '2026-10-30T23:42:00Z'
labels:
  - infra
milestone: m-1
dependencies:
  - TASK-2
ordinal: 5000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Per the 'Schema changes only via new Flyway migrations' decision, the scaffold's ddl-auto=update must be replaced before any real table is introduced in M2, otherwise the first feature migration would be fighting Hibernate's auto-DDL instead of being the single source of truth.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A baseline Flyway migration exists under src/main/resources/db/migration and runs on application startup against Postgres
- [ ] #2 spring.jpa.hibernate.ddl-auto is set to validate
- [ ] #3 mvn spring-boot:run against the docker-compose Postgres starts cleanly with Flyway's migration applied
<!-- AC:END -->
