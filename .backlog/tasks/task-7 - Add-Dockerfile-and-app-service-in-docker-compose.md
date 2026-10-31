---
id: TASK-7
title: Add Dockerfile and app service in docker-compose
status: To Do
assignee: []
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-30T23:42:00Z'
labels:
  - infra
milestone: m-1
dependencies:
  - TASK-5
ordinal: 7000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
docker-compose.yml currently only runs Postgres; per the 'run everything via docker compose' decision the app itself needs to be part of that single command, not run separately via mvn spring-boot:run.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A Dockerfile builds a runnable jar-based image for the app
- [ ] #2 docker-compose.yml gains an app service depending on postgres, using the existing DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD env vars
- [ ] #3 docker compose up brings up both services and the app starts successfully against the compose Postgres
<!-- AC:END -->
