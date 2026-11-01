---
id: TASK-27
title: Add docker compose healthcheck for the app service
status: Done
assignee:
  - '@claude'
created_date: '2026-10-31T11:44:00Z'
updated_date: '2026-11-01T13:04:00Z'
labels:
  - infra
milestone: m-1
dependencies:
  - TASK-8
ordinal: 27000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
docker-compose.yml has a pg_isready healthcheck for postgres (TASK-7) but none for the app service, so 'docker compose ps' cannot show whether the app is actually healthy. Use the /actuator/health endpoint added in TASK-8. Note: the runtime image is eclipse-temurin:25-jre; check whether it ships curl/wget before relying on it in the healthcheck command.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The app service in docker-compose.yml has a healthcheck that calls GET /actuator/health
- [x] #2 docker compose ps shows the app as (healthy) once it has started against the compose Postgres
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Runtime image eclipse-temurin:25-jre (Ubuntu 26.04) has neither curl nor wget; user chose bash's built-in /dev/tcp over installing curl.
2. docker-compose.yml app service: healthcheck running bash -c that sends GET /actuator/health over /dev/tcp/localhost/8080 and greps the body for "status":"UP" (CMD, not CMD-SHELL: /bin/sh is dash, which has no /dev/tcp). interval 10s, timeout 5s, retries 5, start_period 60s for JVM + Flyway startup.
3. Verify: docker compose up -d --build, then docker compose ps shows app (healthy); also check it turns unhealthy-detectable (command exits non-zero when the app is not UP). mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Verified live: docker compose up -d --build -> app 'Up (healthy)', checks exit 0 in ~0.1s, app started in ~5s (well within start_period 60s). Negative checks run inside the container: closed port -> exit 1, response without "status":"UP" -> exit 1. mvn verify passes (no Java changes).

AC #1: docker-compose.yml app.healthcheck sends GET /actuator/health (rendered via docker compose config). AC #2: proven live - docker compose ps showed 'realscore-app-1 ... Up 5 minutes (healthy)' against the compose Postgres; State.Health log all ExitCode 0. No follow-ups.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added a healthcheck for the app service in docker-compose.yml calling GET /actuator/health and requiring "status":"UP" in the body. The eclipse-temurin:25-jre runtime image (Ubuntu 26.04) has no curl or wget, so per the user's choice the request goes through bash's built-in /dev/tcp instead of installing curl; it uses CMD bash -c because CMD-SHELL's /bin/sh is dash, which lacks /dev/tcp. interval 10s, timeout 5s, retries 5, start_period 60s. Verified live (app (healthy), checks ~0.1s) and negatively (closed port and non-UP body both exit 1). No Java, Dockerfile or dependency changes; mvn verify passes (99 tests). Risk: relies on bash staying in the base image.
<!-- SECTION:FINAL_SUMMARY:END -->
