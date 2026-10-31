---
id: TASK-20
title: Parse raw snapshot into normalised listing details
status: To Do
assignee: []
created_date: '2026-10-30T23:43:00Z'
labels:
  - parsing
milestone: m-5
dependencies:
  - TASK-11
documentation:
  - doc-1
ordinal: 20000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implements the idempotent normalisation step from the 'store raw first' decision, turning listing_snapshot JSONB payloads into structured fields using the field-extraction rules catalogued in the 'Sreality API' doc: price (preferring 'Celkova cena'), size, disposition, year built, floor, energy class, construction type, elevator, ownership, service charge, and city/district from the locality string.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Parsing a snapshot produces normalised listing-details fields matching every extraction rule documented in the Sreality API doc's field-extraction section
- [ ] #2 A snapshot with 'Cena na vyzadani' (no parseable price) is parsed without error and yields no price
- [ ] #3 Table-driven tests cover each of the locality-string shapes (3-part, 2-part with kraj, 2-part without kraj, Praha-prefixed) and the price/size/ownership gotchas from the doc's fix-commit history
- [ ] #4 Running the parser twice on the same snapshot produces identical output
<!-- AC:END -->
