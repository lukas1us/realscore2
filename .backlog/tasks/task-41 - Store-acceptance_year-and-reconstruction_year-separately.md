---
id: TASK-41
title: Store acceptance_year and reconstruction_year separately
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T17:32:00Z'
updated_date: '2026-11-01T19:41:00Z'
labels:
  - parsing
milestone: m-7
dependencies: []
type: bug
ordinal: 51000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SrealityListingDetailsExtractor.yearBuilt falls back from acceptance_year to reconstruction_year into the single `year_built` column. A reconstructed panel flat then looks like a new build, which misleads quality scoring and any age-based analysis.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 listing_detail has acceptance_year and reconstruction_year columns, added by a new Flyway migration that replaces year_built
- [x] #2 Each year is parsed only from its own field, without fallback
- [x] #3 A fixture with only reconstruction_year gives acceptance_year NULL and reconstruction_year set
- [x] #4 Existing snapshots are re-parsed so current rows get both columns (README says how if it is a manual step)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Migration V9__split_listing_detail_year_built.sql: add acceptance_year INTEGER and reconstruction_year INTEGER to listing_detail, drop year_built. No SQL backfill (year_built does not say which field it came from, and parsing stays in Java); the reparse fills both columns.
2. Extractor: acceptanceYear = intOrNull(result, acceptance_year), reconstructionYear = intOrNull(result, reconstruction_year); remove yearBuilt and its fallback.
3. Replace yearBuilt with acceptanceYear + reconstructionYear in ListingDetailsFields, ListingDetail (columns, applyFields, toFields), the public ListingDetails record and ListingDetailsQueryService. GET /api/listings/{source}/{externalId} details then show acceptanceYear and reconstructionYear instead of yearBuilt.
4. Fixture detail_reconstruction_year_only.json from a real sale listing with only reconstruction_year (source decided with the user), PII removed. Extractor test: acceptanceYear null, reconstructionYear set. Full-fixture test: acceptanceYear 1975, reconstructionYear null. Replace the fallback test.
5. README: operations note to run POST /api/parsing/reparse?source=sreality once after upgrading (until then both columns are empty), and name the two year fields in the listing endpoint row.
6. mvn verify; rebuild the local app, run the reparse, check counts against the snapshot numbers (3246 acceptance, 1021 reconstruction).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
V9 migration adds acceptance_year and reconstruction_year and drops year_built (no SQL backfill). Fixture detail_reconstruction_year_only.json built from the stored snapshot of sale listing 4059889740 (no live API call), redacted like detail_success.json: user name/email/phones/image, premise web URLs, house numbers. Also added ListingDetailTest.aChangedReconstructionYearIsAChange. mvn verify: 181 tests, 0 failures. Local: Flyway at V9, reparse created 0 / updated 4127 / unchanged 15305 / failed 0; listing_detail now has 3246 acceptance_year, 1021 reconstruction_year, 881 reconstruction-only, matching the snapshot counts; GET /api/listings/sreality/4059889740 shows acceptanceYear null, reconstructionYear 2025.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
author: @claude
created: 2026-11-01T19:34:00Z

---

Data on 2026-11-01 (latest snapshot per listing): both years null 15305, only acceptance_year 3106, only reconstruction_year 881 (these are stored wrongly in year_built today), both 140. All values are JSON numbers in 1835..2029.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
listing_detail.year_built (acceptance_year with a fallback to reconstruction_year) is replaced by separate acceptance_year and reconstruction_year columns (migration V9), each parsed only from its own field, so a reconstructed old flat no longer looks like a new build. The public ListingDetails record, and so the listing endpoint's details, now carry acceptanceYear and reconstructionYear instead of yearBuilt. A new real fixture covers a listing with only reconstruction_year. README operations note says to run the reparse once after upgrading (both columns are empty until then); done locally and checked against the raw snapshot counts. mvn verify passes.
<!-- SECTION:FINAL_SUMMARY:END -->
