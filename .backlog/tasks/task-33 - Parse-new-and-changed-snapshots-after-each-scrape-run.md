---
id: TASK-33
title: Parse new and changed snapshots after each scrape run
status: To Do
assignee: []
created_date: '2026-11-01T12:14:00Z'
labels:
  - parsing
milestone: m-5
dependencies:
  - TASK-21
ordinal: 33000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Nothing calls ListingDetailsParsingService automatically yet: snapshots written by the Sreality scrape job only get listing_detail rows when someone triggers POST /api/parsing/reparse (TASK-21). New and changed listings from each scrape run should be parsed without a manual step, keeping the dependency direction scraping -> listing <- parsing (scraping must not call parsing directly; e.g. an event published by listing, or a parsing-side schedule).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Listings created or changed by a scrape run get an up-to-date listing_detail row without a manual trigger
- [ ] #2 Scraping does not depend on the parsing module (ModularityTests passes)
<!-- AC:END -->
