---
id: TASK-46
title: Sreality floor_number is 1-based but scoring assumes 0 is the ground floor
status: To Do
assignee: []
created_date: '2026-11-05T11:48:00Z'
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
- [ ] #1 The meaning of floor_number (including 0 and negative values) is confirmed against real listings and recorded in the Backlog doc "Sreality API"
- [ ] #2 The stored or exposed floor and the elevator from-floor setting use one consistent, documented convention
- [ ] #3 The missing-elevator penalty applies from the intended storey; tests cover the ground floor and the threshold floor
- [ ] #4 README.md and the property comment describe the convention
<!-- AC:END -->
