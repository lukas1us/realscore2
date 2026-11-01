---
id: TASK-37
title: Parse building condition into listing_detail
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T17:07:00Z'
updated_date: '2026-11-01T21:43:00Z'
labels:
  - parsing
milestone: m-6
dependencies: []
type: feature
ordinal: 47000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Building condition (building_condition, e.g. Novostavba, Po rekonstrukci, Před rekonstrukcí) is one of the strongest value signals in the Sreality detail and is present on every sale flat sampled on 2026-11-01, but it is not parsed. Quality scoring (TASK-35) needs it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 listing_detail has the building condition code and label, added by a new Flyway migration
- [x] #2 The parser fills them from building_condition; a missing or not-entered (value 0) condition is null
- [x] #3 Existing rows are filled by running the reparse endpoint once, as documented in README
- [x] #4 Extractor tests cover a known condition and a missing one
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. V11 migration: add listing_detail.building_condition_code INTEGER and building_condition_label VARCHAR(255), NULL until reparse.
2. ListingDetailsFields, ListingDetail (columns, applyFields, currentFields), ListingDetails + ListingDetailsQueryService.toDetails: add buildingConditionCode/Label (exposed on GET /api/listings as buildingConditionCode/buildingConditionLabel).
3. SrealityListingDetailsExtractor: read building_condition {value,name}; missing or value 0 (nezadáno) -> both null; any other code kept raw with its name (like disposition).
4. Tests: extractor - known condition from detail_success.json (1 Velmi dobrý), value 0 -> null, missing -> null; update ListingDetailTest FIELDS.
5. README: listing endpoint fields + operations note to reparse once.
6. mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
V11 adds building_condition_code/label. Extractor treats value 0 (nezadáno) like a missing field; other codes are kept raw with their name. Reparse test proves existing rows get filled. mvn verify: 204 tests, BUILD SUCCESS.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Building condition is now parsed into listing_detail (building_condition_code, building_condition_label; V11) and exposed on GET /api/listings as buildingConditionCode/Label. Missing or not-entered (0) conditions are null; other codes are stored as Sreality sends them, so TASK-35 can weight by code. Tests: extractor (known codes, 0, missing), entity change detection, reparse filling an existing row. README operations note says to run POST /api/parsing/reparse?source=sreality once. Follow-up: reparse in production before TASK-35 relies on it.
<!-- SECTION:FINAL_SUMMARY:END -->
