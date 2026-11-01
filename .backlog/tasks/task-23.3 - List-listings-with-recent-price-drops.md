---
id: TASK-23.3
title: List listings with recent price drops
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T14:16:00Z'
updated_date: '2026-11-01T16:10:00Z'
labels:
  - analysis
milestone: m-6
dependencies:
  - TASK-23.1
parent_task_id: TASK-23
type: feature
ordinal: 41000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A price drop on an active listing is a sign of a motivated seller and the first thing worth checking when looking for a deal. Finding them should not require going through listings one by one.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 An endpoint lists active listings whose price dropped within a given number of days (default configurable), with the old price, new price, percent drop and drop date
- [x] #2 It can be filtered by sale or rent
- [x] #3 Results are ordered by percent drop, largest first
- [x] #4 README documents the endpoint and its parameters
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Drop definition: over a window of N days, old price = the price in effect when the window started (the last point before it, or the first point if the listing appeared inside the window), new price = current price. A listing is listed when new < old. Drop date = when the current price was first seen. Several drops inside the window add up into one entry.
2. parsing: ListingDetailsQueryService.findActivePriceHistoriesChangedSince(Instant since, Integer transactionTypeCode or null) -> List<ActivePriceHistory> (new public record: source, externalId, transaction type, disposition label, size, city, city part, full price history). Only active listings with a price point since the cutoff.
3. analysis: PriceDrop record (those fields + oldPriceCzk, newPriceCzk, dropPercent 1 decimal, droppedAt) and PriceDropService computing drops (unit-testable), ordered by dropPercent desc, then droppedAt desc.
4. analysis: AnalysisProperties gets priceDropDays (@Positive), application.properties realscore.analysis.price-drop-days=30.
5. analysis: GET /api/analysis/price-drops?days=<n>&type=sale|rent on MarketBenchmarkController or a new PriceDropController; both params optional; days < 1 or unknown type -> 400.
6. Tests: drop computation unit tests (single drop, several drops, drop then rise, drop before the window, new listing in window); endpoint test (removed listings excluded, type filter, order, 400).
7. README: endpoint row with params, config table row.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Plan step 5 settled on a new PriceDropController. Added a public TransactionType enum (SALE=1, RENT=2) in analysis, used in the PriceDrop response and the service API. The parsing query is one SQL join over listing, listing_detail and listing_price_point, filtered by removed_at IS NULL, optional transaction type, and an EXISTS for a point since the cutoff; rows are grouped per listing with a RowCallbackHandler. mvn verify: 130 tests, BUILD SUCCESS.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Adds GET /api/analysis/price-drops?days=&type=sale|rent. Drop = current price below the price in effect at the window start (or the first price if the listing appeared later); several drops add up. parsing: ListingDetailsQueryService.findActivePriceHistoriesChangedSince -> ActivePriceHistory. analysis: TransactionType, PriceDrop.of (old/new price, dropPercent 1 decimal, droppedAt), PriceDropService (largest drop first, then latest), PriceDropController (400 on days < 1 or unknown type). Config realscore.analysis.price-drop-days=30. Tests: PriceDropTest (8), PriceDropControllerTest (4), ActivePriceHistoryQueryTest (2). README: endpoint and config rows. Drops appear only once refetches see lower prices (SCRAPE_REFETCH_AFTER, 7d by default).
<!-- SECTION:FINAL_SUMMARY:END -->
