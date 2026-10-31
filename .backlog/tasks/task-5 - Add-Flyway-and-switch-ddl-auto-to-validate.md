---
id: TASK-5
title: Add Flyway and switch ddl-auto to validate
status: Done
assignee:
  - '@lkrasa'
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-31T11:22:00Z'
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
- [x] #1 A baseline Flyway migration exists under src/main/resources/db/migration and runs on application startup against Postgres
- [x] #2 spring.jpa.hibernate.ddl-auto is set to validate
- [x] #3 mvn spring-boot:run against the docker-compose Postgres starts cleanly with Flyway's migration applied
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add spring-boot-starter-flyway (compile, BOM-managed) and flyway-database-postgresql (runtime, BOM-managed) to pom.xml.
2. Create a baseline migration src/main/resources/db/migration/V1__baseline.sql (empty/no-op — no domain tables exist yet after TASK-2's scaffold removal; real schema comes in later M1/M2 tasks).
3. Set spring.jpa.hibernate.ddl-auto=validate in src/main/resources/application.properties.
4. Start docker-compose Postgres, run mvn spring-boot:run, confirm clean startup with Flyway applying V1 and Hibernate validate passing (no entities to validate against, so this should be a no-op pass).
5. Run mvn verify to confirm tests still pass.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Verified AC3 against a throwaway Postgres container on port 5433 (port 5432 was occupied by an unrelated project's container at the time) since the project's own docker-compose Postgres couldn't bind 5432 initially. App started cleanly: Flyway applied V1__baseline and Hibernate ddl-auto=validate passed (no entities to validate against). Afterwards also verified the real docker-compose.yml stack on port 5432 directly before tearing it down.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added spring-boot-starter-flyway (compile) and flyway-database-postgresql (runtime), both BOM-managed by Spring Boot 4.0.9. Added src/main/resources/db/migration/V1__baseline.sql, a no-op baseline migration (no domain tables exist yet post-TASK-2). Set spring.jpa.hibernate.ddl-auto=validate in src/main/resources/application.properties. Verified mvn verify passes, and that 'mvn spring-boot:run' against a real Postgres 16 instance starts cleanly: Flyway applies V1 and Hibernate validate succeeds. Test application.properties (H2, create-drop) was left untouched — out of scope here, to be addressed by TASK-6's Testcontainers work.
<!-- SECTION:FINAL_SUMMARY:END -->
