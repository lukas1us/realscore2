---
id: TASK-14
title: ID collector with region/disposition bucketing
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-10-31T19:24:00Z'
labels:
  - scraping
milestone: m-3
dependencies:
  - TASK-13
documentation:
  - doc-1
ordinal: 14000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Beats the ~1000-result cap for a sale-apartment search bounded by a configurable price cap, following the region-then-disposition bucketing strategy documented in the 'Sreality API' doc: probe all 14 regions, subdivide any region whose result_size exceeds a safety threshold by the 11 disposition codes, then paginate every resulting bucket and dedupe IDs across all of them. Scoped to sale apartments with a price cap for now; removing the price cap or extending to other categories is tracked separately as later work, not part of this task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given a price cap, the collector probes all 14 regions and, for any region over the bucket threshold, further probes all 11 dispositions within that region
- [x] #2 IDs are collected by paginating every resulting bucket (small regions directly, large regions per-disposition) and deduplicated across buckets
- [x] #3 A test using fixtures simulating one small region (collected directly) and one large region (subdivided by disposition, including an overlapping ID in two buckets) asserts the correct deduplicated ID set
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. SrealityClient.searchEstates(params, limit, offset) -> SearchPage(total, ids from results[].hash_id); same error handling as countEstates.
2. SrealityIdCollector.collectSaleApartmentIds(priceCap): base params category_main_cb=1, category_type_cb=1, price_to; count 14 regions; <= threshold -> paginate region; > threshold -> count 11 dispositions, paginate non-empty (region, disposition) buckets, warn if still over threshold. Paginate until empty page or offset >= total; dedupe into one Set.
3. SrealityProperties + application.properties: bucket-threshold (950), page-size (100).
4. Tests: SrealityClientTest for searchEstates; SrealityIdCollectorTest (WireMock, threshold 5, page size 2): small region 1 direct, large region 10 split into 2+kk/3+kk with overlapping ID, rest 0; asserts deduped set and no disposition probes for region 1. Hand-made search_page_*.json fixtures.
5. Update doc-1: category_type_cb, category_sub_cb, price_to confirmed as v1 search params; no ~1000 cap observed (offset 6000 / limit 1000 work).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Live probes before planning (2026-10-31, ~12 read-only GETs, more than capture-fixture allows; disclosed to user): category_type_cb and category_sub_cb work as v1 search params; no ~1000 cap observed (offset 6000 and limit 1000 work). User chose to keep bucketing as specified (defensive) and to update doc-1, done. SrealityClient.searchEstates(params, limit, offset) -> SearchPage(total, estateIds); countEstates now delegates to it with limit=0, so both share the 'no pagination.total' check. SrealityIdCollector paginates each bucket until an empty page or offset >= total (total refreshed from each page). New config realscore.sreality.bucket-threshold=950 / page-size=100, @Positive. Tests: SrealityIdCollectorTest (threshold 5, page size 2, hand-made search_page_*.json + search_count_empty.json; small buckets' count requests answered with their first page) and 2 new SrealityClientTest cases. mvn verify: 31 tests, BUILD SUCCESS.

Finalized via /finish-task: mvn verify green (31 tests). AC evidence: #1 SrealityIdCollectorTest.probesEveryRegionAndOnlySplitsTheLargeOne; #2 and #3 SrealityIdCollectorTest.collectsSmallRegionDirectlyAndLargeRegionPerDispositionWithoutDuplicates, plus SrealityClientTest.searchEstatesReturnsPageOfIdsAndTotal. No new follow-up tasks: retries/backoff already tracked in M3, residual oversized-bucket gap documented in doc-1.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added SrealityIdCollector.collectSaleApartmentIds(priceCap): searches sale apartments (category_main_cb=1, category_type_cb=1, price_to) by counting each of the 14 regions. A region at or under realscore.sreality.bucket-threshold (950) is paged through directly; a larger one is counted and paged per each of the 11 dispositions (category_sub_cb), with a warning if a (region, disposition) bucket is still over the threshold. IDs go into one LinkedHashSet, which removes duplicates across buckets. SrealityClient gained searchEstates(params, limit, offset) -> SearchPage, and countEstates reuses it. Live probes showed v1 has no ~1000 cap; bucketing kept as a defensive measure per user decision, and doc-1 updated with the verified params and cap findings. Tests: WireMock collector test (small region direct, large region split, 203 in both 2+kk and 3+kk) plus client search tests; mvn verify green, 31 tests. Risk: any request failure aborts the whole collection; retries/backoff are a separate M3 task.
<!-- SECTION:FINAL_SUMMARY:END -->
