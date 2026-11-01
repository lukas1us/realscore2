---
id: TASK-22
title: 'Later: locality/disposition market analysis (median CZK/m^2, rents)'
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-11-01T14:16:00Z'
labels:
  - analysis
milestone: m-6
dependencies: []
ordinal: 22000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Coarse placeholder for the analysis module's first real feature: median price-per-m^2 and rent benchmarks per locality/disposition, replacing the old app's rent_market_scan.py approach documented in the Sreality API doc. Not split into subtasks yet -- do that when this is picked up.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Scope and subtasks are defined when this task is started, informed by whatever listing/parsing data is available by then
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Scoped 2026-11-01 into TASK-22.1 (scrape rentals), TASK-22.2 (parse transaction type/category/citypart), TASK-22.3 (median sale CZK/m2), TASK-22.4 (median rent). Decisions: rents go through the scrape->snapshot->parse pipeline (not live search lookups like the old app); locality = city part for large cities, else city, falling back to district then region below a minimum sample; benchmarks computed on demand, no stored history (later, near TASK-23); rental yield left to TASK-24. Sale price cap (5M) left unchanged - its bias on medians is documented in TASK-22.3, sample counts are exposed.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Split into and delivered TASK-22.1-22.4: rental flats are scraped alongside sales (optional rent price cap); listing_detail records transaction type, property category and city part (migration V6); GET /api/analysis/benchmarks/sale-price-per-m2 and /api/analysis/benchmarks/rent return median sale CZK/m2 and median rent (total and per m2) per locality and disposition, computed on demand with city-part grouping for large cities and fallback to city/district/region below a minimum sample. mvn verify passes. After deploy: run POST /api/parsing/reparse?source=sreality once to fill the new listing_detail columns; the first scrape run fetches all rental details (a few extra hours). Sale medians are biased low in expensive areas by the 5M sale price cap.
<!-- SECTION:FINAL_SUMMARY:END -->
