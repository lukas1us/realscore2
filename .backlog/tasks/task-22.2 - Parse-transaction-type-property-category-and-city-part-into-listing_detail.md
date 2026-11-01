---
id: TASK-22.2
title: Parse transaction type, property category and city part into listing_detail
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T12:41:00Z'
updated_date: '2026-11-01T14:16:00Z'
labels:
  - parsing
milestone: m-6
dependencies: []
documentation:
  - doc-1
parent_task_id: TASK-22
type: feature
ordinal: 35000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Once rentals are scraped, listing_detail cannot tell a sale from a rent, nor a flat from another category, so market medians would mix them. City-wide medians are also too coarse for large cities (Praha, Brno, Ostrava); the raw v1 payload already carries locality.citypart (e.g. "Plzeň 2-Slovany", while district is "Plzeň-město"), which is not parsed yet.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 listing_detail records the transaction type (category_type_cb) and property category (category_main_cb) of each listing
- [x] #2 listing_detail records locality.citypart
- [x] #3 Schema change is a new Flyway migration; re-running the reparse endpoint fills the new columns for existing snapshots
- [x] #4 Extractor tests cover the new fields, including when they are missing
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Migration V6: listing_detail gets transaction_type_code INTEGER, property_category_code INTEGER, citypart VARCHAR(255), all nullable.
2. ListingDetailsFields + ListingDetail: three new fields; applyFields/sameValues cover them via the record.
3. Extractor: category_type_cb.value, category_main_cb.value (raw codes, like disposition_code), locality.citypart.
4. Tests: extractor (sale flat, rental commercial, missing fields), ListingDetail change detection, reparse fills the new columns on an old row. mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Stored raw Sreality codes rather than enums: only 1=prodej/2=pronájem and 1=byty are confirmed (doc-1); 2-5 categories are unverified, so mapping them to names would assert more than is known. After deploy, run POST /api/parsing/reparse?source=sreality to fill existing rows.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
listing_detail now records transaction_type_code (category_type_cb), property_category_code (category_main_cb) and citypart (locality.citypart), via new migration V6, so analysis can separate sales from rentals and flats from other types, and group large cities by city part. Existing rows stay NULL until re-parsed; the reparse endpoint fills them (tested). Tests: extractor on sale-flat and rental-commercial fixtures and missing fields, ListingDetail change detection, reparse fills new columns. mvn verify passes. Follow-up for the operator: run the reparse once after deploy.
<!-- SECTION:FINAL_SUMMARY:END -->
