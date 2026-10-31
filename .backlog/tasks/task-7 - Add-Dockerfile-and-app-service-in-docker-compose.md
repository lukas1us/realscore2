---
id: TASK-7
title: Add Dockerfile and app service in docker-compose
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-31T11:39:00Z'
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
- [x] #1 A Dockerfile builds a runnable jar-based image for the app
- [x] #2 docker-compose.yml gains an app service depending on postgres, using the existing DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD env vars
- [x] #3 docker compose up brings up both services and the app starts successfully against the compose Postgres
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add multi-stage Dockerfile (maven:3-eclipse-temurin-25 build with -DskipTests, eclipse-temurin:25-jre runtime, non-root, port 8080)
2. Add .dockerignore (target, .git, .idea, .backlog, .claude, *.iml)
3. docker-compose.yml: pg_isready healthcheck on postgres; app service built from ., depends_on postgres service_healthy, DB_* env vars, 8080:8080, restart unless-stopped
4. Update README/CLAUDE.md run instructions
5. Verify: docker compose build, docker compose up -d, check app logs for Flyway + startup, mvn verify
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Verified: docker compose build app succeeds; docker compose up -d -> postgres healthy, app Up on 8080, logs show Flyway 'Successfully applied 1 migration ... now at version v1' and 'Started RealscoreApplication in 1.957 seconds' against jdbc:postgresql://postgres:5432/realscore. mvn verify: BUILD SUCCESS. Image build uses -DskipTests (tests run on host). README/CLAUDE.md run instructions updated.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added a multi-stage Dockerfile (Maven/Temurin 25 build, Temurin 25 JRE runtime, non-root, port 8080) and .dockerignore, and an app service in docker-compose.yml that waits for a new pg_isready healthcheck on postgres and connects via DB_HOST=postgres plus the existing DB_* vars. Per decision-7, docker compose up now brings up the whole system. Verified manually: image builds, both containers come up, app logs show Flyway migrating to v1 and 'Started RealscoreApplication' against the compose Postgres; mvn verify green. Tests are skipped inside the image build (run on host via mvn verify). README and CLAUDE.md run instructions updated. No follow-ups.
<!-- SECTION:FINAL_SUMMARY:END -->
