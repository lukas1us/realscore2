---
id: TASK-27
title: Add docker compose healthcheck for the app service
status: To Do
assignee: []
created_date: '2026-10-31T11:44:00Z'
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
- [ ] #1 The app service in docker-compose.yml has a healthcheck that calls GET /actuator/health
- [ ] #2 docker compose ps shows the app as (healthy) once it has started against the compose Postgres
<!-- AC:END -->
