---
id: TASK-35
title: Add quality factors to the listing score
status: To Do
assignee: []
created_date: '2026-11-01T16:20:00Z'
updated_date: '2026-11-01T16:20:00Z'
labels:
  - analysis
milestone: m-6
dependencies:
  - TASK-24.2
type: feature
ordinal: 45000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The v1 score (TASK-24.2) looks only at price against the market, rental yield and price drops. Parsed details that also affect value are ignored: energy class, ownership (personal vs cooperative), construction type (brick vs panel), floor and elevator. Weighting them needs judgement calls, so they were kept out of v1.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The chosen factors and how each moves the score are agreed before implementation and documented
- [ ] #2 A listing missing a factor is not penalised for it
- [ ] #3 The score breakdown shows the contribution of each factor
<!-- AC:END -->
