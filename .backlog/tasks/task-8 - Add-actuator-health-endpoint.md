---
id: TASK-8
title: Add actuator health endpoint
status: To Do
assignee: []
created_date: '2026-10-30T23:42:00Z'
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
- [ ] #1 spring-boot-starter-actuator is added and GET /actuator/health returns 200 with status UP when the app and its DB are healthy
- [ ] #2 An integration test hits the endpoint and asserts the UP status
<!-- AC:END -->
