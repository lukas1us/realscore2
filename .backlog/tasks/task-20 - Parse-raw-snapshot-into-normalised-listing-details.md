---
id: TASK-20
title: Parse raw snapshot into normalised listing details
status: Done
assignee:
  - '@lkrasa'
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-10-31T17:39:00Z'
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
- [x] #1 Parsing a listing_snapshot payload produces normalised fields: price in CZK (from price_summary_czk, treating 0/missing as no price), size in m^2 (usable_area, falling back to floor_area), disposition code and label (category_sub_cb), year built (acceptance_year, falling back to reconstruction_year), floor (floor_number), energy class (energy_efficiency_rating_cb), construction type (building_type), elevator (boolean from elevator.value), ownership (OV/DV/other from ownership.value), and city/district/region (direct locality.city/district/region fields)
- [x] #2 A snapshot with price_summary_czk = 0 (Sreality's 'price on request' case, confirmed in doc-1) is parsed without error and yields no price
- [x] #3 Table-driven tests cover a fully-populated listing, the price-on-request case, and each ownership value (OV/DV/state-or-unknown)
- [x] #4 Running the parser twice on the same snapshot produces identical output
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add a minimal public read capability to the listing module (needed since parsing depends on listing, not the reverse, and TASK-11 only wrote snapshots): a ListingSnapshotView record and ListingSnapshotService.findLatestSnapshot(source, externalId) returning the listing id + raw payload.
2. Add V4__create_listing_detail.sql: listing_detail (UNIQUE listing_id FK to listing, price_czk, size_m2, disposition_code/label, year_built, floor, energy_class, construction_type, elevator, ownership, city/district/region, parsed_at).
3. Add a pure SrealityListingDetailsExtractor (com.realscore.parsing.internal) parsing a raw JSON payload string into a ListingDetailsFields value object, per the rewritten doc-1 rules: price_summary_czk (0/missing -> no price), usable_area/floor_area, category_sub_cb, acceptance_year/reconstruction_year, floor_number, energy_efficiency_rating_cb, building_type, elevator, ownership (OV/DV/other), locality.city/district/region.
4. Add internal ListingDetail entity + repository, and a public ListingDetailsParsingService.parse(source, externalId) that reads the latest snapshot via ListingSnapshotService, runs the extractor, and upserts (find-or-create by listing_id) the listing_detail row.
5. Unit-test the extractor with table-driven cases (fully-populated listing, price-on-request, each ownership value) using real captured JSON (detail_success.json + a hand-built price-on-request/ownership variant).
6. Testcontainers-backed test proving ListingDetailsParsingService.parse() run twice on the same snapshot produces exactly one listing_detail row with identical values (idempotency).
7. mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Hit a Jackson package mismatch: Spring Boot 4.0.9 auto-configures a Jackson 3 (tools.jackson.databind.ObjectMapper) bean, not the classic com.fasterxml.jackson one (which is also on the classpath transitively, e.g. via Testcontainers, but not auto-configured as a bean) - had to switch SrealityListingDetailsExtractor's imports to tools.jackson.*. Also hit a test-isolation bug: ListingDetailsParsingServiceTest initially reused external ID '1', colliding with the pre-existing ListingSnapshotServiceTest under the shared Testcontainers Postgres instance (Spring's test-context caching reuses the same container across test classes with identical @SpringBootTest+@Import config) - fixed by using a distinct external ID (2494705740, the real captured fixture's estate ID) and scoping the assertion to findByListingId rather than a global repository count.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
author: @lkrasa
created: 2026-10-31T17:24:00Z

---

ACs rewritten 2026-10-31: the original ACs described the old (pre-v1) extraction methodology (items-list fallbacks, locality-string disambiguation) which is explicitly obsolete per doc-1's rewrite - v1 has no items list and locality is already a structured object. New ACs target the real v1 direct-field shape. While rewriting, resolved two doc-1 open questions live: (1) results item shape (confirmed, TASK-20), (2) price_summary_czk vs price_czk authority - price_summary_czk is always the total; price_czk can be a per-unit rate (confirmed via a 450 CZK/m^2/month listing where price_summary_czk = 450 x usable_area). Also confirmed the 'price on request' shape: price_czk and price_summary_czk both 0, no dedicated flag. Service charge ('fond oprav') extraction is dropped from scope - no confirmed direct field was found for it in the fixtures checked; flagged as a known gap, not silently dropped.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Rewrote the ACs (see comments) to match the real v1 API before implementing, since the originals described the obsolete pre-v1 items-list/locality-string extraction methodology. Added: V4__create_listing_detail.sql (one row per listing, overwritten on re-parse, FK to listing). com.realscore.listing: ListingSnapshotView + ListingSnapshotService.findLatestSnapshot(source, externalId), the minimal public read capability parsing needs (TASK-11 only wrote). com.realscore.parsing.internal: pure SrealityListingDetailsExtractor (raw JSON string -> ListingDetailsFields) implementing the confirmed v1 rules - price_summary_czk as the authoritative total price (0/missing = no price, confirmed live against real 'price on request' commercial listings), usable_area/floor_area, category_sub_cb, acceptance_year/reconstruction_year, floor_number, energy_efficiency_rating_cb, building_type, elevator, ownership (OV/DV/OTHER), and direct locality.city/district/region fields - plus the ListingDetail entity/repository. com.realscore.parsing: public ListingDetailsParsingService.parse(source, externalId) wiring read -> extract -> upsert. Along the way, resolved two doc-1 open questions live (results item shape; price_summary_czk vs price_czk authority) and captured a new scrubbed detail_price_on_request.json fixture. Service charge ('fond oprav') extraction is a known, flagged gap - no confirmed v1 field was found for it. mvn verify passes (27 tests, including ModularityTests with no boundary violations).
<!-- SECTION:FINAL_SUMMARY:END -->
