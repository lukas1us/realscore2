---
id: TASK-9
title: Add CI pipeline
status: Done
assignee:
  - '@lkrasa'
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-31T11:57:00Z'
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
- [x] #1 A GitHub Actions workflow runs mvn verify (including the Modulith and Testcontainers-backed tests) on push and pull_request
- [x] #2 The workflow is demonstrated passing on a real commit/PR
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create .github/workflows/ci.yml: checkout (actions/checkout@v7), setup-java (actions/setup-java@v6, temurin, Java 25, maven cache), run 'mvn --batch-mode --no-transfer-progress verify' on push and pull_request. GitHub-hosted ubuntu runners have Docker preinstalled, needed for the Testcontainers Postgres tests.
2. Verify the workflow YAML is well-formed and the chosen action versions/Java version actually exist.
3. Ask before pushing (CLAUDE.md reserves push to the user) — need a real push/PR for AC2's demonstration. Once pushed, check the run via gh (after auth) or ask the user to confirm/share the run URL.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Pinned actions/checkout@v7 and actions/setup-java@v6 (confirmed current via GitHub tags API) with distribution=temurin, java-version=25 (confirmed supported). Verified locally with the exact CI command: 'mvn --batch-mode --no-transfer-progress verify' passes. AC2 (demonstrated passing on a real commit/PR) is pending: push is reserved for the user per CLAUDE.md git rules — waiting for them to push this commit, then I'll check the Actions run.

Demonstrated on commit 1a34eb0: both push (https://github.com/lukas1us/realscore2/actions/runs/41198596691) and pull_request (https://github.com/lukas1us/realscore2/actions/runs/41198734937) runs completed successfully; mvn verify step passed on ubuntu-latest, confirming Modulith + Testcontainers-backed tests run in CI.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added .github/workflows/ci.yml running 'mvn verify' on push and pull_request (actions/checkout@v7, actions/setup-java@v6 with Temurin Java 25 and maven cache). Verified locally first with the exact CI command. User pushed commit 1a34eb0; both triggered GitHub Actions runs (push and pull_request) completed successfully, with the mvn verify step green — confirming the Modulith boundary and Testcontainers-backed tests now run automatically in CI.
<!-- SECTION:FINAL_SUMMARY:END -->
