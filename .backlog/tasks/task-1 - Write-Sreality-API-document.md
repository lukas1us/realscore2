---
id: TASK-1
title: Write Sreality API document
status: Done
assignee: []
created_date: '2026-10-30T23:39:00Z'
updated_date: '2026-10-30T23:39:00Z'
labels:
  - docs
milestone: m-0
dependencies: []
documentation:
  - doc-1
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The old realscoreCZ Python app (itself almost entirely AI-generated) encodes a lot of trial-and-error knowledge about Sreality's undocumented internal JSON API: endpoints, query parameters, region/disposition code tables, the ~1000-result cap and its region/disposition bucketing workaround, rate limiting and retry behaviour, and numerous parsing gotchas recorded as fix: commits (price field choice, locality string parsing, Praha/Brno district handling, price-on-request skipping). Once this is captured, the old repo should never need to be consulted again for Sreality domain knowledge.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 doc-1 (Sreality API) covers: both API endpoints and their params; the region (1-14) and disposition code tables; the ~1000-result cap and the region-then-disposition bucketing workaround; rate limiting and retry behaviour (429 backoff, 403/404 skip); all detail-field parsing gotchas found in the old app's fix: commits
- [x] #2 Any claim that could not be verified directly from the old app's code (vs. inferred, assumed, or only asserted in a comment) is explicitly marked [UNVERIFIED] in the document
- [x] #3 Bezrealitky and iDNES are mentioned as existing, HTML-based, and out of scope for v1, with no further analysis
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Extracted from realscoreCZ: backend/scrapers/sreality.py, sreality_search.py, constants.py, jobs/full_market_scan.py, jobs/rent_market_scan.py, and the relevant fix: commits (0a6863f, 9c6b318, 195735c, 35db3f3, 13a5b96). Found and flagged an unresolved axis-labeling inconsistency between sreality.py's _canonical_url (seo block) and sreality_search.py's confirmed search-API params.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created doc-1 'Sreality API' covering both endpoints, search params (with the category_main_cb/category_type_cb axis clarified and a flagged [UNVERIFIED] inconsistency against sreality.py's canonical-URL builder), the region and disposition code tables, the ~1000-result cap and bucketing workaround, rate limiting/retry rules, all detail-field parsing gotchas from the old app's fix commits, rental benchmark lookup behaviour, and the two biggest open risks (API is undocumented/reverse-engineered; ToS/robots.txt never checked). Bezrealitky/iDNES noted as out of scope.
<!-- SECTION:FINAL_SUMMARY:END -->
