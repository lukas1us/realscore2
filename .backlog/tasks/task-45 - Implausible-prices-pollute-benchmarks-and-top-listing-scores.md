---
id: TASK-45
title: Implausible prices pollute benchmarks and top listing scores
status: To Do
assignee: []
created_date: '2026-11-05T11:48:00Z'
labels:
  - parsing
  - analysis
dependencies: []
priority: high
type: bug
ordinal: 58000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Sellers miscategorise listings or use placeholder prices, and those listings currently go straight into the medians and scores. Examples from the DB on 2026-11-05: sale flat 1741094988 (Mariánské Lázně, 56 m²) priced 12,500 CZK with `price_unit_cb` "za měsíc" (a rent listed as a sale) is #3 in its city with total score 85.5, discount 99.6 % and rental yield 1086 %. Two Litvínov sales at 134,500 and 269,000 CZK score 81.8 with 67 % yield. Rentals priced at 1 CZK (Most) or 300 CZK (5× Šestajovice, `price_czk_m2` 6), and rentals at 6,893,000 / 870,000 / 780,000 CZK a month (sale prices listed as rent). There are also sizes of 8–11 m² for 1+kk sales at 1.3–2.5M and 575–5731 m² for ordinary rentals. `price_unit_cb` alone is not reliable either: 85 rentals marked "za nemovitost" have normal monthly rents (median 17,000), and one "za měsíc" sale is priced at 2.8M. Medians resist a few outliers, but the per-listing discount and yield do not, so these rise to the top of `/api/analysis/scores`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A sale or rental whose price or size is implausible is excluded from sale and rent benchmarks
- [ ] #2 Such a listing gets no score, or is clearly flagged, instead of ranking near the top of /api/analysis/scores
- [ ] #3 The examples in the description (12,500 CZK sale, 1 CZK and 300 CZK rents, 6.9M CZK rent) are all treated as implausible, and ordinary listings are not
- [ ] #4 The plausibility thresholds are configurable and documented in README.md
- [ ] #5 Raw snapshots are not changed; the rule is re-applied to existing data by a reparse or on demand
- [ ] #6 Tests cover sale and rent cases on both sides of each threshold
<!-- AC:END -->
