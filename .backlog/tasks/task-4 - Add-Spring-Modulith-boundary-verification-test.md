---
id: TASK-4
title: Add Spring Modulith boundary verification test
status: Done
assignee:
  - '@lkrasa'
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-31T10:29:00Z'
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
- [x] #1 A test calling ApplicationModules.of(RealscoreApplication.class).verify() exists and passes on the current (empty) module skeleton
- [x] #2 Introducing a one-off illegal cross-module dependency on an internal class causes this test to fail (verified during implementation, not left in the codebase)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add spring-modulith-bom 2.0.9 (exact Spring Boot 4.0.9 match) to dependencyManagement, and spring-modulith-starter-test as a test-scope dependency.
2. Add a test (e.g. ModularityTests) calling ApplicationModules.of(RealscoreApplication.class).verify().
3. Run mvn test to confirm it passes on the current empty module skeleton.
4. Temporarily introduce an illegal cross-module dependency (e.g. a class in one module's internal package referencing another module's internal package) to confirm the test fails, then remove it — not left in the codebase.
5. Run mvn verify to confirm the final state passes.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Added spring-modulith-bom 2.0.9 (exact match for Spring Boot 4.0.9, confirmed via GitHub release notes) and spring-modulith-starter-test. Verified AC2 manually: added a temporary internal-to-internal cross-module reference (scraping -> listing.internal), confirmed ModularityTests failed with a clear violation message, then removed the temporary files.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added spring-modulith-bom 2.0.9 (import scope, matches Spring Boot 4.0.9 exactly) and spring-modulith-starter-test (test scope) to pom.xml. Added ModularityTests.verifiesModuleBoundaries(), which calls ApplicationModules.of(RealscoreApplication.class).verify() and passes on the current empty module skeleton. Verified during implementation (not left in the codebase) that introducing an illegal cross-module dependency on an internal class (scraping referencing listing.internal) makes the test fail with a clear violation message, then removed those temporary files. 'mvn verify' passes on the final state.
<!-- SECTION:FINAL_SUMMARY:END -->
