---
id: TASK-57
title: Leave out-of-scope listings out of price drops
status: To Do
assignee: []
created_date: '2026-11-06T21:58:00Z'
labels:
  - analysis
dependencies: []
priority: low
type: bug
ordinal: 70000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Since TASK-47, a listing that is still online but no longer returned by the searches (price raised above `SCRAPE_PRICE_CAP`, switched to price on request, category changed) gets `listing.out_of_scope_since` and is left out of benchmarks and scores. `GET /api/analysis/price-drops` (`PriceDropService`, via `ListingDetailsQueryService.findActivePriceHistoriesChangedSince`) still filters only on `removed_at`, so it can list such listings. Their price history is also only refreshed at the out-of-scope recheck cadence (`SCRAPE_OUT_OF_SCOPE_RECHECK_AFTER`, 7 days), so a drop shown for them can be stale, and the list no longer matches the sample the rest of the analysis uses. Example: a sale raised above the cap and later lowered to just above it shows as a price drop although it is outside the collected sample.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A listing marked out of scope is not listed by `GET /api/analysis/price-drops`, for sale or rent
- [ ] #2 A listing that returns to scope is listed again when it has a drop in the window
- [ ] #3 Tests cover an out-of-scope listing with a drop and one back in scope
- [ ] #4 README.md describes that price drops leave out out-of-scope listings
<!-- AC:END -->
