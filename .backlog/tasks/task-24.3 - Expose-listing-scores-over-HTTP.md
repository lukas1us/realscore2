---
id: TASK-24.3
title: Expose listing scores over HTTP
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T16:20:00Z'
updated_date: '2026-11-01T16:58:00Z'
labels:
  - analysis
milestone: m-6
dependencies:
  - TASK-24.2
parent_task_id: TASK-24
type: feature
ordinal: 44000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Scores (TASK-24.2) are only useful once they can be browsed: a ranked list to find the best deals, and the score shown when looking at one listing.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 GET /api/analysis/scores returns scored listings best first, limited by a limit parameter with a configurable default
- [x] #2 The list can be filtered by region, city and disposition
- [x] #3 GET /api/listings/{source}/{externalId} includes the score breakdown for an active sale flat that can be scored, and null otherwise
- [x] #4 Invalid parameters return 400
- [x] #5 README documents the endpoint, its parameters and the scoring configuration
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. AnalysisProperties: scoresLimit (@Positive), realscore.analysis.scores-limit=50.
2. ListingScoreService: score(ListingOverview) -> Optional<ListingScore> for one listing, scored the same way as scoreAll() (shared private scoring path; drop computed from the listing's own price history via PriceDrop.of). Only an active flat for sale is scored.
3. ListingScoreService.top(limit, region, city, disposition): scoreAll() filtered (exact, case-insensitive match on region, city and disposition label such as 2+kk), then limited.
4. New ListingScoreController: GET /api/analysis/scores?limit=&region=&city=&disposition=; all optional; limit < 1 -> 400.
5. ListingResponse gets a score field (ListingScore or null); ListingOverviewService fills it via score(overview).
6. Tests: endpoint (best first, limit and default, each filter, 400), listing endpoint (score for a scorable sale flat, null for a rental and a removed flat).
7. README: scores endpoint row, score in the listing endpoint row, scores-limit config row, How it works line.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented as planned. ListingScoreService now maps both ActiveListingDetails and ListingOverview to a private Candidate so scoreAll() and score(overview) share one scoring path; the single-listing drop uses PriceDrop.of on the listing's own history with PriceDropService.windowStart. Filters are exact and case-insensitive and run before the limit. In URLs the disposition label needs + encoded as %2B (documented in README). The single-listing score still computes all benchmarks once per request. mvn verify: 156 tests, BUILD SUCCESS.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Adds GET /api/analysis/scores?limit=&region=&city=&disposition= (best first, default limit realscore.analysis.scores-limit=50, limit < 1 -> 400) and a score field in GET /api/listings/{source}/{externalId} (null unless an active, scorable flat for sale). ListingScoreService gains top(...) and score(ListingOverview). Tests: ListingScoreControllerTest (5). README: endpoint rows, config row, intro and How it works.
<!-- SECTION:FINAL_SUMMARY:END -->
