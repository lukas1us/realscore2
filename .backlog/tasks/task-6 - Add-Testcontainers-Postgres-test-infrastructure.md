---
id: TASK-6
title: Add Testcontainers Postgres test infrastructure
status: Done
assignee:
  - '@lkrasa'
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-31T11:33:00Z'
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
- [x] #1 A reusable test base or configuration starts a Postgres Testcontainer and runs Flyway migrations against it
- [x] #2 One smoke test using it passes, proving the container-backed context loads successfully
- [x] #3 The H2 test dependency and its application.properties override are removed
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add test-scope deps: org.testcontainers:testcontainers-postgresql, org.testcontainers:testcontainers-junit-jupiter, org.springframework.boot:spring-boot-testcontainers (all BOM-managed, Testcontainers 2.0.5 / Boot 4.0.9). Remove the H2 test dependency.
2. Add a reusable @TestConfiguration (e.g. PostgresContainerConfig) with a @Bean @ServiceConnection PostgreSQLContainer, importable by any test needing a real Postgres-backed context; Flyway + ddl-auto=validate run automatically against it via existing auto-config.
3. Update RealscoreApplicationTests to @Import the config as the smoke test proving the container-backed context loads (satisfies AC2).
4. Remove src/test/resources/application.properties (pure H2 override, superseded by the container's auto-wired connection details).
5. Run mvn verify to confirm everything passes with the real container.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Testcontainers 2.0.5 (BOM-managed by Spring Boot 4.0.9) renamed every module artifact with a testcontainers- prefix (e.g. org.testcontainers:postgresql -> org.testcontainers:testcontainers-postgresql) and moved/un-genericized PostgreSQLContainer to org.testcontainers.postgresql.PostgreSQLContainer (no longer generic — confirmed by compiling with -Dmaven.compiler.showDeprecation=true against the old org.testcontainers.containers.PostgreSQLContainer, which is deprecated).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added test-scope deps (spring-boot-testcontainers, testcontainers-junit-jupiter, testcontainers-postgresql; all BOM-managed) and removed H2. Added PostgresContainerConfig, a reusable @TestConfiguration with a @Bean @ServiceConnection PostgreSQLContainer — any test can @Import it to get a real Postgres-backed context with Flyway migrations and ddl-auto=validate applied automatically via the existing production auto-config. RealscoreApplicationTests now imports it as the smoke test (AC2). Deleted src/test/resources/application.properties (its H2 datasource + ddl-auto=create-drop override is superseded by the container's auto-wired connection details and the main validate setting). mvn verify passes: real Postgres 16 container starts, Flyway applies V1, context loads.
<!-- SECTION:FINAL_SUMMARY:END -->
