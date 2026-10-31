---
id: TASK-10
title: Sreality client fetches one estate detail
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-31T12:46:00Z'
labels:
  - scraping
milestone: m-2
dependencies:
  - TASK-9
documentation:
  - doc-1
ordinal: 10000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The first real feature slice: talk to Sreality's detail endpoint and get raw JSON back, with nothing else attached yet (no persistence, no bulk collection). Keeping this isolated makes it easy to verify the HTTP/parsing boundary in isolation before building anything on top of it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A client in com.realscore.scraping calls GET https://www.sreality.cz/api/v1/estates/{id} and returns the raw response body/JSON
- [x] #2 A WireMock test using a fixture JSON under src/test/resources/fixtures verifies a successful fetch
- [x] #3 A non-200 response (e.g. 404) raises a typed exception rather than propagating a generic HTTP client error
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add org.wiremock:wiremock-standalone:3.13.2 (test scope, pinned; shaded to avoid Jetty 12.0 / Jackson 2 clashes with Boot 4)
2. scraping.internal: SrealityProperties (@ConfigurationProperties realscore.sreality: baseUrl, userAgent), SrealityConfiguration (@EnableConfigurationProperties)
3. scraping.internal: SrealityClient (RestClient, doc-1 headers) with String fetchEstateDetail(long) -> GET /api/v1/estates/{id}, raw body returned unchanged
4. scraping.internal: SrealityApiException(estateId, statusCode) thrown for any non-200 status
5. SrealityClientTest (WireMock extension, no Spring context): success vs fixture + request path/headers; 404 and 500 -> SrealityApiException
6. Verify: dependency:tree, mvn verify (incl. ModularityTests)
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Added org.wiremock:wiremock-standalone 3.13.2 (test, version in <wiremock.version>); dependency:tree shows it alone with no transitive deps, so no Jetty 12.0 / Jackson 2 clash with Boot 4. Runs fine on Java 25. Implemented SrealityProperties/SrealityConfiguration/SrealityClient/SrealityApiException in scraping.internal; client returns the v1 detail body unchanged and maps any non-200 status to SrealityApiException(estateId, statusCode). SrealityClientTest (WireMock, no Spring context): fixture round-trip + request headers, 404 and 500 -> SrealityApiException. mvn verify: 6 tests, BUILD SUCCESS (context loads with the new bean, ModularityTests green). Not handled (out of scope): connection errors (TASK-15), body-level status_code != 200 with HTTP 200 (unknown whether v1 does this).
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
author: @lkrasa
created: 2026-10-31T12:04:00Z

---

Blocked: the documented endpoint (GET https://www.sreality.cz/api/cs/v2/estates/{id}, per doc-1) no longer exists. Confirmed via curl: both the detail endpoint (tried estate ID 2494705740, then trivial ID 1) and the search endpoint (/api/cs/v2/estates with minimal params) return an identical generic nginx 404 (not a Sreality JSON error) — same result for a trivial/likely-valid ID as for the originally requested one. Also tried /api/cs/v3/estates (404). The site itself (https://www.sreality.cz/) loads fine (200), so this is isolated to the v2 estates API path, not a site-wide block or auth requirement. Looking for the current API path before any client code is written against doc-1's stale endpoint.

---

author: @lkrasa
created: 2026-10-31T12:16:00Z

---

Further investigation: confirmed the whole /api/cs/v2/estates path is gone even for real, live listings (tested against sitemap-sourced IDs 2672870748 and 2751566428, not just the originally given 2494705740). The site is a Next.js app; real listing data is embedded in each detail page's HTML as a __NEXT_DATA__ JSON blob (props.pageProps.dehydratedState.queries[] where queryKey[0]==="estate") — confirmed working on a live listing, with a different (camelCase) field shape than doc-1 describes (priceCzk, categoryMainCb, params instead of items, etc.). Found the client's own code builds the request as C.q(["estates", id], {lang, region_tip, preview}) but couldn't pin down the exact base path/prefix from the minified JS bundles without diminishing-returns effort. User will check real browser devtools (Network tab) on a live listing to get the exact current API URL; will update doc-1 and this task's AC #1 once that's confirmed.

---

author: @lkrasa
created: 2026-10-31T12:22:00Z

---

User found the real endpoint via browser devtools (search variant): GET https://www.sreality.cz/api/v1/estates/search?category_main_cb=1&locality_region_id=2,11,7,6&multilocality_type_or=true&price_to=5000000&ownership=1&building_type=2,1,3,4,6,7,8&limit=0&offset=0 — confirmed 200 JSON. By analogy I confirmed the detail endpoint too: GET https://www.sreality.cz/api/v1/estates/{id} returns 200 with {"result": {...}}. The originally-requested ID 2494705740 DOES exist under this path (a 1+1 apartment in Plzeň) — it only 404'd under the old /api/cs/v2/estates/{id} path, which is fully retired. Captured and scrubbed the fixture at src/test/resources/fixtures/sreality/detail_success.json.

Response shape differs substantially from doc-1:
- No more 'items' list of {name,value} facts — nearly every field doc-1's 'Field-by-field extraction rules' section maps from items is now a direct, named, typed field: usable_area, floor_area, floor_number, elevator, ownership, energy_efficiency_rating_cb, building_type (NEW - construction type, not disposition), etc.
- Coded fields are now self-describing objects {name: <human label>, value: <code>} instead of a bare code needing an external lookup table, e.g. category_sub_cb={'name':'1+1','value':3} (confirms disposition codes UNCHANGED from doc-1's table), ownership={'name':'Osobní','value':1} (resolves doc-1's ownership/ownership_type_cb ambiguity - the real field is just 'ownership'), category_main_cb/category_type_cb values also confirmed unchanged (byty=1, prodej=1).
- 'locality' is now a structured object (city, citypart, district, region, street, gps_lat/lon, ids, seo_name per level) instead of a comma-separated string - doc-1's whole city/district parsing-by-part-count section is obsolete.
- Pagination: search uses limit/offset (not per_page/page) and pagination.total (not result_size); limit=0 cleanly returns just the count (replaces the old per_page=1 probe hack).
- locality_region_id now accepts a direct comma-separated list in one param (+ a new multilocality_type_or=true/false flag for AND/OR semantics) instead of repeating the param per doc-1.
- PII found and scrubbed in the fixture: user.user_name/user_email/user_phones/image (broker contact), premise.web_url/web_visible_url (broker's personal domain, contains his name). advert_description free text had no embedded contact info in this sample.

doc-1 needs a substantial rewrite given the scale of these changes (new base path, new response shape, resolved ambiguities, obsoleted parsing-rules section) - proposing that as a separate step, not editing it silently. TASK-10's AC #1 also names the old endpoint explicitly and should be updated to the v1 path.

---

author: @lkrasa
created: 2026-10-31T12:36:00Z

---

doc-1 rewritten to describe the real v1 API (GET /api/v1/estates/{id} detail, GET /api/v1/estates/search), with old-API-dependent sections marked [UNVERIFIED - NOT RE-TESTED] rather than deleted (bucketing/cap workaround, rental benchmark lookups). TASK-10's AC #1 updated to the v1 detail endpoint. No longer blocked - ready to implement against the captured fixture (src/test/resources/fixtures/sreality/detail_success.json).

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added the first scraping slice: SrealityClient (scraping.internal) fetches GET /api/v1/estates/{id} via RestClient with doc-1's headers and returns the raw body unchanged, ready to be stored as a snapshot in TASK-12. Any non-200 status becomes SrealityApiException(estateId, statusCode) so TASK-15 can branch on status for retry/skip. Base URL and User-Agent are configurable via realscore.sreality.* (defaults to live Sreality). Added wiremock-standalone 3.13.2 (test) - shaded, so no Jetty/Jackson clash with Boot 4; works on Java 25. SrealityClientTest covers fixture round-trip + headers, 404 and 500. mvn verify green (6 tests). Not handled on purpose: connection/timeout errors (TASK-15) and a possible HTTP-200-with-error-body status_code (unverified whether v1 does this).
<!-- SECTION:FINAL_SUMMARY:END -->
