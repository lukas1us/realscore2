---
id: TASK-35
title: Add quality factors to the listing score
status: To Do
assignee:
  - '@claude'
created_date: '2026-11-01T16:20:00Z'
updated_date: '2026-11-01T17:32:00Z'
labels:
  - analysis
milestone: m-6
dependencies:
  - TASK-36
  - TASK-37
  - TASK-38
  - TASK-39
  - TASK-40
  - TASK-41
  - TASK-42
type: feature
ordinal: 45000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The v1 score (TASK-24.2) looks only at price against the market, rental yield and price drops. Parsed details that also affect value are ignored: energy class, ownership (personal vs cooperative), construction type (brick vs panel), floor and elevator. Weighting them needs judgement calls, so they were kept out of v1.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The chosen factors and how each moves the score are agreed before implementation and documented
- [ ] #2 A listing missing a factor is not penalised for it
- [ ] #3 The score breakdown shows the contribution of each factor
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Agreed with the user on 2026-11-01 (before implementation):
- Quality is a 4th score part: starts at 50, each factor adds or subtracts points, clamped to 0-100. A missing or unreliable factor adds 0. The breakdown shows each factor's points.
- Ownership: OV +10, DV -10, other 0 (DV is cheaper because harder to mortgage, so its discount looks better than it is).
- Construction: Cihlová +10, Panelová -5, others 0.
- Energy class: A +10, B +7, C +3, D 0, E -3, F -7; G and missing 0 (G is what sellers without a certificate must state; 41% of sale flats on 2026-11-01).
- Building condition (TASK-37): Novostavba / Po rekonstrukci +10, Velmi dobrý +5, Dobrý 0, Před rekonstrukcí -10, Špatný -20, Ve výstavbě / Projekt / V rekonstrukci 0.
- No elevator at floor >= 4: -10, only for an explicit "Ne" (TASK-36).
- Year built left out (missing for 75%).
- Weights discount / rental yield / price drop / quality: 45 / 30 / 10 / 15. All points and weights configurable.
- Depends on TASK-36 (not-entered elevator and energy class as null) and TASK-37 (building condition parsed).
<!-- SECTION:NOTES:END -->
