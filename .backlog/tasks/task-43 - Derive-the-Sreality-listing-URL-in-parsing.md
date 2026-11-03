---
id: TASK-43
title: Derive the Sreality listing URL in parsing
status: Done
assignee:
  - '@claude'
created_date: '2026-11-02T21:50:00Z'
updated_date: '2026-11-03T21:11:00Z'
labels:
  - parsing
  - analysis
dependencies: []
documentation:
  - doc-1 - Sreality API
ordinal: 56000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Scores, benchmarks and price drops point at listings, but no endpoint links to the listing on Sreality, so checking a result means searching for it by hand. The raw detail payload has everything needed to build the public detail URL, so parsing can derive it. That keeps scraping raw-only and lets a re-parse fill it in for existing listings.

URL shape: https://www.sreality.cz/detail/{type}/{main}/{sub}/{locality}/{hash_id}
- type: result.category_type_cb.value (1 prodej, 2 pronajem)
- main: result.category_main_cb.value (1 byt, 2 dum, 3 pozemek, 4 komercni, 5 ostatni)
- sub: result.category_sub_cb.name (e.g. 1+1)
- locality: result.locality city_seo_name, citypart_seo_name, street_seo_name joined with "-". Skip missing or blank parts and drop exact repeats.
- hash_id: result.hash_id

Open question for the Sreality API doc: does Sreality resolve a detail page by ID alone (/detail/-/-/-/-/{id})? The user will check this in a browser and report the result.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Parsing src/test/resources/fixtures/sreality/detail_success.json yields exactly https://www.sreality.cz/detail/prodej/byt/1+1/plzen-plzen-2-slovany-barakova/2494705740
- [x] #2 A fixture with no street and no citypart still yields a valid URL (no empty segments, no doubled or trailing hyphens)
- [x] #3 Repeated locality parts appear only once in the slug
- [x] #4 The URL is stored in a new listing_detail.url column added by a new Flyway migration
- [x] #5 The analysis endpoints that return listings include the URL, and README.md documents the new field
- [x] #6 After a re-parse of existing snapshots, every stored listing_detail row whose payload has the required fields has a URL
- [x] #7 The Sreality API doc records whether a detail page resolves by ID alone (/detail/-/-/-/-/{id}), based on the user's browser check, and says the slug is cosmetic if it does
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Migration V10 (via new-migration skill): add nullable listing_detail.url TEXT. The next reparse fills it, as V9 did for the year columns.
2. parsing/internal: new SrealityListingUrl builds https://www.sreality.cz/detail/{type}/{main}/{sub}/{locality}/{hash_id} from the detail result. Type and main come from fixed code maps, sub is category_sub_cb.name as given, and locality is city, citypart and street seo names with blank ones skipped and exact repeats dropped. If an unknown type or main code, a blank sub name, no hash_id or no locality part at all is found, the URL is null.
3. Add url to ListingDetailsFields, the extractor, the ListingDetail entity and its change detection.
4. Expose url in ListingDetails, ActiveListingDetails, ActivePriceHistory and their queries, then in the analysis output: ListingScore and PriceDrop (GET listing gets it through details).
5. Tests: the extractor gives the exact AC URL for detail_success.json, a new fixture without street or citypart, dedup, and null cases. Reparse fills url on a row parsed before it. Listing, score and price-drop controller tests show url.
6. README: url in endpoint responses, plus an operations note to run the reparse after upgrading.
7. Sreality API doc: URL derivation and the result of the user's /detail/-/-/-/-/{id} browser check.
8. With approval, run POST /api/parsing/reparse?source=sreality on the local compose app so existing listings get the URL.

Revision after the 404 check: the slug matters, so step 2 normalises sub (lowercase, accents stripped, spaces to hyphens; "1+1" and "1+kk" stay unchanged). A listing missing a required part gets a null URL rather than "-" placeholders. The doc marks slugs for layouts other than N+1/N+kk as unverified.

Correction: the migration is V13 (V10-V12 already exist).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
User browser check (2026-11-03): https://www.sreality.cz/detail/-/-/-/-/2494705740 returns 404, so Sreality does not resolve a detail page by ID alone and the slug must be correct. Confirmed real URLs: https://www.sreality.cz/detail/prodej/byt/1+1/plzen-plzen-2-slovany-barakova/2494705740 and https://www.sreality.cz/detail/pronajem/byt/1+kk/plzen-jizni-predmesti-magisterska/4088148044.

Implemented: V13 adds listing_detail.url; SrealityListingUrl builds it during extraction; url flows through ListingDetails/ActiveListingDetails/ActivePriceHistory into the listing, score and price-drop endpoints. Fixed during work: Map.of().get(null) throws, so missing category codes are checked before lookup (existing payload-less tests caught it). New fixture detail_no_street_or_citypart.json (rental 1+kk, city only). mvn verify: 263 tests, BUILD SUCCESS. AC6 covered by ListingDetailsReparseServiceTest.reparseFillsTheUrlOfRowsParsedBeforeIt; the reparse of the real local DB is still pending user approval.

Local DB (2026-11-03): rebuilt app, V13 applied, POST /api/parsing/reparse?source=sreality -> created 0, updated 20569, unchanged 0, failed 0 in 27 s. All 20569 listing_detail rows now have a url; both browser-checked URLs match exactly. Non-N+kk/N+1 slugs produced: 6-a-vice, atypicky, pokoj, dum/rodinny (still unverified on the site).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Parsing now derives the public Sreality detail URL (https://www.sreality.cz/detail/{type}/{main}/{sub}/{locality}/{hash_id}) from the raw detail payload and stores it in listing_detail.url (Flyway V13). The listing, score and price-drop endpoints return it. Sreality returns 404 for an ID-only URL, so the slug must be right: sub is lowercased with accents stripped and spaces turned into hyphens, blank locality parts are skipped, exact repeats are dropped, and no URL is built when a segment is unknown. Verified by mvn verify (263 tests) and by a reparse of the local DB (20569/20569 rows got a URL, the two browser-checked URLs match exactly). Risk: slugs for sub names other than N+1/N+kk (6-a-vice, atypicky, pokoj, rodinny) are not confirmed on the site; the Sreality API doc marks them unverified.
<!-- SECTION:FINAL_SUMMARY:END -->
