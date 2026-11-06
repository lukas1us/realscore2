---
id: TASK-46
title: Sreality floor_number is 1-based but scoring assumes 0 is the ground floor
status: Done
assignee:
  - '@claude'
created_date: '2026-11-05T11:48:00Z'
updated_date: '2026-11-06T21:10:00Z'
labels:
  - parsing
  - analysis
dependencies: []
priority: medium
type: bug
ordinal: 59000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`listing_detail.floor` stores Sreality `floor_number` as-is, and `realscore.analysis.scoring.quality.elevator.from-floor` is documented as "0 is the ground floor". The data says otherwise. Of 4452 listings with floor_number 1, 933 descriptions say "v přízemí/přízemní byt" and 444 say "v 1. patře". Floor 2 mostly says "1. patře" (784) and floor 3 "2. patře" (589). Only 180 listings have floor 0, and only 58 of those mention přízemí. So Sreality appears to count podlaží from 1 = ground floor. The missing-elevator penalty therefore applies one storey lower than configured, and the floor shown in score factors and the listing endpoint is off by one compared with the usual Czech "patro". Floors 0 and below (-4…0) also exist and need a defined meaning.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The meaning of floor_number (including 0 and negative values) is confirmed against real listings and recorded in the Backlog doc "Sreality API"
- [x] #2 The stored or exposed floor and the elevator from-floor setting use one consistent, documented convention
- [x] #3 The missing-elevator penalty applies from the intended storey; tests cover the ground floor and the threshold floor
- [x] #4 README.md and the property comment describe the convention
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Keep listing_detail.floor as Sreality floor_number (Czech NP numbering: 1 = 1. NP = ground floor; 0 = ground/raised ground as entered by some sellers; negative = underground, -1 = 1. PP). No migration or reparse.
2. Change default realscore.analysis.scoring.quality.elevator.from-floor 4 -> 5 so the missing-elevator penalty still starts at the 4th floor above ground (4. patro = 5. NP); update the property comment and ScoringProperties.Elevator Javadoc.
3. Record the convention and the evidence (description wording vs floor_number counts) in Backlog doc 'Sreality API'.
4. QualityScorerTest: ground floor (1), floor below threshold (4) and threshold floor (5) with the default config.
5. README: from-floor row and the floor field in the listing endpoint describe the NP convention.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Confirmed against the latest snapshot of every listing (advert_description wording vs floor_number): floor_number is the Czech podlaží (NP) number, 1 = ground floor; 0 = ground/raised ground as some sellers enter it; negative = underground (-1 = 1. PP). Evidence recorded in doc 'Sreality API'. Option A (approved): listing_detail.floor stays the raw floor_number (no migration, no reparse); elevator.from-floor default 4 -> 5 so the missing-elevator penalty still starts at 4. patro as originally intended. Javadoc of ScoringProperties.Elevator, QualityScorer.Factors and ListingDetails, the property comment and README (config row + Operations note on floors) describe the convention. QualityScorerTest covers ground floor (1), 0, -1, below threshold (4), threshold (5) and above (6). mvn verify green (280 tests).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Sreality floor_number was confirmed to be the 1-based Czech NP number (1 = ground floor) and documented in doc 'Sreality API', README and Javadoc. The stored floor is unchanged; the elevator from-floor default moved from 4 to 5 so the missing-elevator penalty applies from the 4th floor above ground as intended. Tests cover ground and threshold floors. Effect: listings on 4th NP (3. patro) without elevator no longer lose 10 quality points.
<!-- SECTION:FINAL_SUMMARY:END -->
