---
id: TASK-8
title: Add actuator health endpoint
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-31T11:47:00Z'
labels:
  - infra
milestone: m-1
dependencies:
  - TASK-2
ordinal: 8000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A scheduled, mostly-unattended scraping app needs a cheap way to confirm it's actually up, both for local docker compose use and for future CI/deployment checks.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 spring-boot-starter-actuator is added and GET /actuator/health returns 200 with status UP when the app and its DB are healthy
- [x] #2 An integration test hits the endpoint and asserts the UP status
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add spring-boot-starter-actuator (BOM-managed, no version) to pom.xml
2. No property changes: default exposure is health only; DB indicator auto-enabled via DataSource
3. Add HealthEndpointTests: @SpringBootTest(RANDOM_PORT) + PostgresContainerConfig, RestClient GET /actuator/health, assert 200 and status UP
4. Verify: dependency:tree, mvn verify, manual curl against docker compose
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Added spring-boot-starter-actuator (BOM-managed 4.0.9, no conflicts in dependency:tree). No property changes needed: default exposure is health only; DataSource health indicator is auto-enabled. HealthEndpointTests (RANDOM_PORT + Testcontainers Postgres, RestClient) asserts 200 and status UP. mvn verify: 3 tests, BUILD SUCCESS. Manual: docker compose up -> curl /actuator/health = {"groups":["liveness","readiness"],"status":"UP"} HTTP 200. Follow-up: TASK-27 (compose healthcheck for app).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added spring-boot-starter-actuator (BOM-managed, no version pin) so GET /actuator/health gives a cheap liveness check for the unattended scraper; the DataSource health indicator is auto-enabled, so UP covers the DB too. Default exposure (health only) was kept, no property changes. HealthEndpointTests starts the app on a random port against Testcontainers Postgres and asserts 200 + status UP via RestClient (no extra test dependency). mvn verify green (3 tests); manually confirmed against docker compose. Follow-up: TASK-27 (compose healthcheck for the app service).
<!-- SECTION:FINAL_SUMMARY:END -->
