---
id: TASK-35
title: Add quality factors to the listing score
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T16:20:00Z'
updated_date: '2026-11-02T20:16:00Z'
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
- [x] #2 A listing missing a factor is not penalised for it
- [x] #3 The score breakdown shows the contribution of each factor
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. parsing: ActiveListingDetails + ListingDetailsQueryService.findActive SELECT gain ownership, construction_type, energy_class, building_condition_code, floor, elevator.
2. analysis/internal ScoringProperties: new nested quality {weight, ownership{personal,cooperative}, construction{brick,panel}, energy-class{a..g}, building-condition{new-build,after-reconstruction,very-good,good,before-reconstruction,poor,under-construction,project,in-reconstruction}, elevator{from-floor,missing}}. ASCII keys; Sreality values mapped to keys in code.
3. analysis/internal QualityScorer: per factor -> points (missing/unknown/unmapped -> 0); sub-score = clamp(50 + sum, 0, 100).
4. ScoreCalculator: quality is a 4th always-known part in the weighted average; still empty when neither discount nor yield is known.
5. ListingScore gains quality: QualityScorePart(score, weightPercent, points, factors[{factor, value, points}]) - all 5 factors always listed. ListingScoreService.Candidate carries the factor fields from both sources.
6. application.properties: weights 45/30/10/15 + quality points as agreed.
7. Tests: QualityScorerTest (each factor, missing -> 0, clamp, elevator only on explicit false at floor>=from-floor), ScoreCalculatorTest, ScoringPropertiesTest, ListingScoreServiceTest/ControllerTest breakdown.
8. README: scores endpoint + config table. mvn verify.
<!-- SECTION:PLAN:END -->

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

Building condition is mapped by Sreality code (verified against local snapshots on 2026-11-01: 1 Velmi dobrý, 2 Dobrý, 3 Špatný, 4 Ve výstavbě, 5 Projekt, 6 Novostavba, 8 Před rekonstrukcí, 9 Po rekonstrukci, 10 V rekonstrukci); 7 K demolici and unknown codes add 0. Floor 0 is the ground floor. Config keys are ASCII English names; the Sreality values are mapped to them in QualityScorer. mvn verify: 239 tests, BUILD SUCCESS.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Score gains a 4th part, quality (weight 15; others now 45/30/10): 50 plus points for ownership, construction, energy class, building condition and an explicitly missing elevator at floor >= 4, clamped to 0-100. Missing or unlisted values add 0. Every points value and weight is configurable under realscore.analysis.scoring.quality. The scores and listing endpoints show a quality part with each factor's value and points. ActiveListingDetails now carries the factor fields. Tests: QualityScorerTest (each factor, missing, clamp, elevator rule), ScoreCalculatorTest/ScoringPropertiesTest for the new part and weights, service/controller breakdown. README and CLAUDE.md updated. Building condition needs the TASK-37 reparse to count.
<!-- SECTION:FINAL_SUMMARY:END -->
