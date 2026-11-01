---
id: TASK-41
title: Store acceptance_year and reconstruction_year separately
status: To Do
assignee: []
created_date: '2026-11-01T17:32:00Z'
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
- [ ] #1 listing_detail has acceptance_year and reconstruction_year columns, added by a new Flyway migration that replaces year_built
- [ ] #2 Each year is parsed only from its own field, without fallback
- [ ] #3 A fixture with only reconstruction_year gives acceptance_year NULL and reconstruction_year set
- [ ] #4 Existing snapshots are re-parsed so current rows get both columns (README says how if it is a manual step)
<!-- AC:END -->
