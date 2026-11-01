---
id: TASK-36
title: Parse not-entered elevator and energy class as unknown
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-01T17:07:00Z'
updated_date: '2026-11-01T17:32:00Z'
labels:
  - parsing
milestone: m-7
dependencies: []
type: bug
ordinal: 46000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Sreality sends a placeholder when the seller left a field empty: elevator {"name": "- nezadáno", "value": 0} and energy class {"name": "- vyber třídu", "value": 0}. The parser turns the first into elevator = false (same as an explicit "Ne") and the second into energy class "-". On 2026-11-01 that was 3,606 and 1,218 of 8,172 sale flats. Quality scoring (TASK-35) must not penalise a field that was simply not entered.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Elevator value 0 (not entered) is parsed as null; 1 stays true and 2 stays false
- [ ] #2 Energy class value 0 (no class chosen) is parsed as null; A-G stay as they are
- [ ] #3 Extractor tests cover the not-entered cases
- [ ] #4 README tells to run the reparse endpoint once after upgrading so existing rows are corrected
- [ ] #5 Energy class is mapped from its value code, not from the name text
- [ ] #6 No listing_detail.energy_class is '-' after a reparse
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. SrealityListingDetailsExtractor.elevator: value 1 -> true, 2 -> false, 0 / any other / missing -> null (today anything but 1 is false).
2. SrealityListingDetailsExtractor.energyClass: value 0 -> null; otherwise first letter of the name as today. Decide by value, not by the "- vyber třídu" text.
3. ListingDetail.applyFields already treats changed values as an update, so a reparse corrects existing rows (UPDATED count).
4. Tests in SrealityListingDetailsExtractorTest: elevator Ano / Ne / nezadáno / missing, energy class value 0 and a normal class; inline minimal payloads.
5. README operations note: run POST /api/parsing/reparse?source=sreality once after upgrading.
<!-- SECTION:PLAN:END -->

## Comments

<!-- COMMENTS:BEGIN -->
author: @claude
created: 2026-11-01T17:32:00Z

---

Scope extended on 2026-11-01 (moved to M7): first list the distinct elevator and energy class values in listing_snapshot.raw (the user pastes the query output), then map both by value/code, not by the name text (energy class no longer name.substring(0,1)). Plan step 2 is revised once the distinct values are known.

---
<!-- COMMENTS:END -->
