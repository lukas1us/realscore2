---
id: TASK-4
title: Add Spring Modulith boundary verification test
status: To Do
assignee: []
created_date: '2026-10-30T23:42:00Z'
labels:
  - infra
milestone: m-1
dependencies:
  - TASK-3
ordinal: 4000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The one-way dependency rule (scraping to listing to parsing to analysis, internal subpackages never used across modules) is only useful if a build breaks when it's violated. Spring Modulith's ApplicationModules.verify() does exactly that, and should be in place before any module has real code, so every module is born under the constraint instead of being retrofitted to it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A test calling ApplicationModules.of(RealscoreApplication.class).verify() exists and passes on the current (empty) module skeleton
- [ ] #2 Introducing a one-off illegal cross-module dependency on an internal class causes this test to fail (verified during implementation, not left in the codebase)
<!-- AC:END -->
