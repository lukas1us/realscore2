---
id: TASK-43
title: Derive the Sreality listing URL in parsing
status: To Do
assignee: []
created_date: '2026-11-02T21:50:00Z'
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
- [ ] #1 Parsing src/test/resources/fixtures/sreality/detail_success.json yields exactly https://www.sreality.cz/detail/prodej/byt/1+1/plzen-plzen-2-slovany-barakova/2494705740
- [ ] #2 A fixture with no street and no citypart still yields a valid URL (no empty segments, no doubled or trailing hyphens)
- [ ] #3 Repeated locality parts appear only once in the slug
- [ ] #4 The URL is stored in a new listing_detail.url column added by a new Flyway migration
- [ ] #5 The analysis endpoints that return listings include the URL, and README.md documents the new field
- [ ] #6 After a re-parse of existing snapshots, every stored listing_detail row whose payload has the required fields has a URL
- [ ] #7 The Sreality API doc records whether a detail page resolves by ID alone (/detail/-/-/-/-/{id}), based on the user's browser check, and says the slug is cosmetic if it does
<!-- AC:END -->
