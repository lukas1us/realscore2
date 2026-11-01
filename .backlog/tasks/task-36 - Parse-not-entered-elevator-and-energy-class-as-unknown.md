---
id: TASK-36
title: Parse not-entered elevator and energy class as unknown
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T17:07:00Z'
updated_date: '2026-11-01T19:02:00Z'
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
- [x] #1 Elevator value 0 (not entered) is parsed as null; 1 stays true and 2 stays false
- [x] #2 Energy class value 0 (no class chosen) is parsed as null; A-G stay as they are
- [x] #3 Extractor tests cover the not-entered cases
- [x] #4 README tells to run the reparse endpoint once after upgrading so existing rows are corrected
- [x] #5 Energy class is mapped from its value code, not from the name text
- [x] #6 No listing_detail.energy_class is '-' after a reparse
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. SrealityListingDetailsExtractor.elevator: switch on value: 1 -> true, 2 -> false, 0 / any other / missing -> null (today anything but 1 is false).
2. SrealityListingDetailsExtractor.energyClass: map by value code, not name text: 1..7 -> "A".."G"; 0 (no class chosen), any other code or missing -> null. Drop name.substring(0,1).
3. ListingDetail.applyFields already treats changed values as an update, so a reparse corrects existing rows (UPDATED count).
4. Tests in SrealityListingDetailsExtractorTest (inline minimal payloads): parameterized elevator 1/2/0 -> true/false/null + missing -> null; parameterized energy class codes 0..7 -> null, A..G + unknown code 99 and missing -> null; existing full-fixture test keeps C / true.
5. README operations note: say explicitly to run POST /api/parsing/reparse?source=sreality once after this upgrade so not-entered elevator and energy class become NULL.
6. AC #6: covered by the extractor test (code 0 never yields '-'); with your OK, rebuild the app container and run the reparse on the local DB, then confirm SELECT count(*) FROM listing_detail WHERE energy_class = '-' is 0.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Elevator and energy class now map by value code (elevator 1/2 -> true/false; energy class 1..7 -> A..G; 0, other codes and missing -> null). mvn verify: 172 tests, 0 failures. Rebuilt the local app container and ran POST /api/parsing/reparse?source=sreality: created 0, updated 9421, unchanged 10011, failed 0. Afterwards listing_detail: energy_class '-' = 0, energy_class NULL = 3476, elevator NULL = 8483 / true = 7371 / false = 3578, matching the raw snapshot code counts.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
author: @claude
created: 2026-11-01T17:32:00Z

---

Scope extended on 2026-11-01 (moved to M7): first list the distinct elevator and energy class values in listing_snapshot.raw (the user pastes the query output), then map both by value/code, not by the name text (energy class no longer name.substring(0,1)). Plan step 2 is revised once the distinct values are known.

---

author: @claude
created: 2026-11-01T18:28:00Z

---

Distinct values in listing_snapshot (2026-11-01): elevator value 0 '- nezadáno' (8483 snapshots), 1 Ano (7371), 2 Ne (3578); no snapshot lacks the field. energy_efficiency_rating_cb value 0 '- vyber třídu' (3476), 1..7 = A..G (G alone 8213). No other codes. listing_detail currently has 3476 rows with energy_class '-'.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Not-entered elevator ('- nezadáno', code 0) and not-chosen energy class ('- vyber třídu', code 0) are now parsed as NULL instead of false and '-', so quality scoring (TASK-35) can tell 'not entered' apart from 'no elevator' or a real class. Both fields are mapped from the value code; energy class no longer uses the first letter of the name text. Extractor tests cover every code seen in the data, an unknown code and a missing field. The README operations note says to run the reparse once after upgrading. Verified with mvn verify and a reparse of the local DB (no '-' left). Follow-up idea, not done here: G is ~44% of the classes entered and may often mean 'no certificate'; worth considering in TASK-35.
<!-- SECTION:FINAL_SUMMARY:END -->
