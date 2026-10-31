---
id: TASK-9
title: Add CI pipeline
status: To Do
assignee: []
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-30T23:42:00Z'
labels:
  - infra
milestone: m-1
dependencies:
  - TASK-4
  - TASK-6
ordinal: 9000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
There is currently no CI for this repo (unlike the old Python app, which had a GitHub Actions workflow). Given the Modulith boundary test and Testcontainers-based tests are the main safety net for the architecture decisions in this roadmap, they need to run automatically on every change, not just when a developer remembers to run mvn verify locally.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A GitHub Actions workflow runs mvn verify (including the Modulith and Testcontainers-backed tests) on push and pull_request
- [ ] #2 The workflow is demonstrated passing on a real commit/PR
<!-- AC:END -->
