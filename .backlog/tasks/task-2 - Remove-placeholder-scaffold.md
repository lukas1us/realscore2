---
id: TASK-2
title: Remove placeholder scaffold
status: Done
assignee: []
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-31T09:37:00Z'
labels:
  - infra
milestone: m-1
dependencies: []
ordinal: 2000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The current model/repository/controller/service scaffold (Property, PropertyRepository, PropertyController, PropertyScoringService) is a US-style price-per-sqft/beds/baths placeholder explicitly called out in CLAUDE.md as not to be built on. It has no place in the target domain (Czech real estate listings scraped from portals) and should be cleared out before the real package structure is introduced, so nothing is ever tempted to extend it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 model/, repository/, controller/ packages and PropertyScoringService (plus their tests) no longer exist
- [x] #2 mvn test passes with no references to the removed classes anywhere in src
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Delete src/main/java/com/realscore/model/Property.java and the now-empty model/ package.
2. Delete src/main/java/com/realscore/repository/PropertyRepository.java and the now-empty repository/ package.
3. Delete src/main/java/com/realscore/controller/PropertyController.java and the now-empty controller/ package.
4. Delete src/main/java/com/realscore/service/PropertyScoringService.java, src/test/java/com/realscore/service/PropertyScoringServiceTest.java, and the now-empty service/ packages (both main and test).
5. Leave RealscoreApplication.java, RealscoreApplicationTests.java (plain context-load test, no Property references), and both application.properties files untouched -- ddl-auto/Flyway changes belong to TASK-5, not this task.
6. Run mvn test and confirm it passes with zero references to the removed classes.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Removed via git rm: model/Property.java, repository/PropertyRepository.java, controller/PropertyController.java, service/PropertyScoringService.java, and its test service/PropertyScoringServiceTest.java (both src/main and src/test). Verified no remaining references with grep -rl Property src (no matches) and mvn verify (passing, 0 JPA repositories found, context loads).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Deleted the placeholder model/repository/controller/service scaffold (Property, PropertyRepository, PropertyController, PropertyScoringService + its test) entirely, via git rm. Left RealscoreApplication, RealscoreApplicationTests (plain context-load test), and both application.properties files untouched -- ddl-auto/Flyway changes are TASK-5's job, not this one. mvn verify passes cleanly with zero references to the removed classes. Review: just confirm the deletion is the full scaffold and nothing else should go with it before TASK-3 introduces the real com.realscore module packages.
<!-- SECTION:FINAL_SUMMARY:END -->
