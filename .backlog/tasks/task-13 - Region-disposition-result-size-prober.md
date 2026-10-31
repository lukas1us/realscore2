---
id: TASK-13
title: Region/disposition result-size prober
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-10-31T13:38:00Z'
labels:
  - scraping
milestone: m-3
dependencies:
  - TASK-10
documentation:
  - doc-1
ordinal: 13000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The bucketing workaround for Sreality's ~1000-result cap needs a cheap way to ask 'how many results would this query match' before deciding whether to subdivide. See the 'Sreality API' doc's cap/bucketing section for the per_page=1 probe technique this is based on.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A function probes GET /api/v1/estates/search with limit=0 for a given set of search params and returns the parsed pagination.total
- [x] #2 A WireMock test using a fixture response asserts pagination.total is parsed correctly
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Update ACs to the v1 search API (retired /api/cs/v2 per doc-1): limit=0, pagination.total
2. SrealityApiException: keep (estateId, statusCode) constructor; add (requestPath, statusCode) for non-estate requests; estateId becomes nullable Long, requestPath recorded for both
3. SrealityClient.countEstates(Map<String,String> searchParams): GET /api/v1/estates/search with params + limit=0&offset=0, returns pagination.total; non-200 -> SrealityApiException(requestPath, status)
4. SrealityClientTest: count parsed from fixtures/sreality/search_count_only.json (9561) with query params asserted; non-200 search -> SrealityApiException with null estateId
5. mvn verify
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
ACs rewritten to the v1 search API (old /api/cs/v2 path retired, see doc-1). SrealityClient.countEstates(Map<String,String>) calls GET /api/v1/estates/search with the given params plus limit=0&offset=0 and returns pagination.total, parsed into a private record (Jackson 3 ignores the other fields by default, no annotation needed). Non-200 -> SrealityApiException(requestPath, statusCode): new constructor per review, existing (estateId, statusCode) one unchanged; estateId is now a nullable Long (null for searches) and requestPath is recorded for both. Tests: count from fixtures/sreality/search_count_only.json (Praha flats, total 9561) with query params + header asserted; 429 on search -> exception with requestPath and null estateId. mvn verify: 15 tests, BUILD SUCCESS. Note for TASK-14: Praha alone is 9561, far over the ~950 bucket threshold, and category_sub_cb as a v1 search param is still unverified (doc-1).

Per review: request paths stay in code (part of the API contract; base URL is the configurable part). SrealityApiException converted to Lombok @Getter + @Accessors(fluent = true) per the new CLAUDE.md Lombok rule, keeping estateId()/requestPath()/statusCode() names. mvn verify: 15 tests, BUILD SUCCESS.

Review fix: countEstates no longer NPEs on an empty 200 body, and Pagination.total is now Long so a missing total fails instead of silently becoming 0 (which would make TASK-14 skip a bucket). Both cases throw IllegalStateException('... no pagination.total'); not SrealityApiException, since the status was 200. Two new tests. mvn verify: 17 tests, BUILD SUCCESS.

Per review (Sonar S1075 'URIs should not be hardcoded', user chose making them configurable): detail path, search path and Referer moved into SrealityProperties alongside base URL and User-Agent; all defaults now in application.properties under realscore.sreality.* (no @DefaultValue literals in Java), record is @Validated with @NotBlank so missing config fails at startup. SrealityApiException's estate constructor is now (estateId, requestPath, statusCode); both error handlers pass request.getURI().getPath() of the failed request, so the exception holds no path literal. Detail 404 test also asserts requestPath. Follow-ups created: TASK-29 (empty detail body -> NPE in recordFetch), TASK-30 (Lombok cleanup of older code). mvn verify: 17 tests, BUILD SUCCESS.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added the cheap result-size probe the bucketing strategy needs, against the v1 API (ACs rewritten; /api/cs/v2 is retired per doc-1): SrealityClient.countEstates(Map<String,String>) calls the configured search path with the given params plus limit=0&offset=0 and returns pagination.total. Fails loudly instead of silently: non-200 -> SrealityApiException(requestPath, statusCode) (new constructor, estateId null for searches); a 200 without pagination.total -> IllegalStateException rather than an NPE or a silent 0 that would make TASK-14 skip a bucket. Per review (Sonar S1075), detail path, search path and Referer joined base URL and User-Agent as realscore.sreality.* settings in application.properties, validated with @NotBlank; the exception now takes the failed request's actual path. SrealityApiException uses Lombok fluent getters. Tests: count parsed from fixtures/sreality/search_count_only.json (9561) with params asserted, 429 on search, empty body, missing total. mvn verify green (17 tests). Follow-ups: TASK-29 (empty detail body), TASK-30 (Lombok cleanup). Risk for TASK-14: category_sub_cb is still unverified as a v1 search param.
<!-- SECTION:FINAL_SUMMARY:END -->
