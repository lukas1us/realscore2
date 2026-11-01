---
id: TASK-40
title: Strip seller PII before hashing and storing snapshots
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T17:32:00Z'
updated_date: '2026-11-01T20:14:00Z'
labels:
  - scraping
  - listing
milestone: m-7
dependencies: []
type: bug
ordinal: 50000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Raw snapshots store the seller's contact data (`result.user` holding user_email and user_phones, plus `premise.web_url` and `premise.web_visible_url`). It's personal data the app does not need. decision-2 says payloads are stored as-is, so the change needs a recorded decision.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 result.user, premise.web_url and premise.web_visible_url are removed from the payload before it is hashed and inserted
- [x] #2 A new Flyway migration scrubs existing listing_snapshot rows
- [x] #3 A decision amending decision-2 is recorded
- [x] #4 No listing_snapshot.raw contains user_email or user_phones (test)
- [x] #5 After the migration, the next run does not create a new snapshot for an unchanged listing only because PII was stripped
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. New scraping/internal SrealityPayloadScrubber (Jackson): removes result.user, result.premise.web_url and result.premise.web_visible_url and re-serialises. Applied in SrealityClient.fetchDetail, so Found carries the scrubbed body; the bulk path and the removal check both go through it.
2. payload_hash becomes SHA-256 of Postgres' jsonb text form of the payload: ListingSnapshotRepository native query encode(sha256(convert_to(CAST(:payload AS jsonb)::text, 'UTF8')), 'hex'), used by ListingSnapshotService.recordFetch instead of the Java digest. SQL can then compute the same hash, and whitespace or key order no longer counts as a change.
3. Migration V10__strip_seller_contact_data_from_listing_snapshot.sql: one UPDATE that removes the three paths with #- and recomputes payload_hash with the same expression.
4. decision-8 'Strip seller contact data from raw snapshots before storing' amending decision-2.
5. Tests: scrubber unit test; client test (Found body has no user_email/user_phones); ListingSnapshotService: same JSON with other whitespace/key order is UNCHANGED; migration test: snapshot with PII and old-style hash inserted via JDBC, V10 script run, then no user_email/user_phones in payload and fetching the same raw response gives UNCHANGED (no new snapshot).
6. README: snapshots are stored without the seller's contact data.
7. mvn verify; rebuild local app (Flyway V10), check no payload contains user_email/user_phones.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented as planned. SrealityPayloadScrubber returns the payload untouched when it has none of the fields, reads decimals as BigDecimal so numbers are written back exactly, and turns invalid JSON into IllegalStateException (counted per listing, does not abort a run). decision-8 created with the CLI; its body was written into the file once, approved by the user, because the CLI cannot set it. Tests: SrealityPayloadScrubberTest, client and fetcher tests (stored payload has no user_email/user_phones/web URLs), ListingSnapshotServiceTest (whitespace/key order = UNCHANGED), SrealityEstateFetcherTest.scrubMigrationRemovesContactDataAndTheNextFetchOfTheSameResponseWritesNoSnapshot (AC #5). Two SrealityScrapeJobTest seeds now use the scrubbed fixture. mvn verify: 198 tests, 0 failures. Local: V10 applied in 39 s; 19432 snapshots, 0 with contact data or the removed fields, 0 hashes off the new definition; table now 914 MB on disk (dead tuples) until vacuumed.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
author: @claude
created: 2026-11-01T20:01:00Z

---

Data on 2026-11-01: 19432 snapshots (430 MB). result.user is an object in 18524 (user_email/user_phones present in all of them) and null in 908; 18524 have premise web URLs. payload_hash is SHA-256 of the response string as received, so SQL cannot recompute it after scrubbing (jsonb reorders keys and drops whitespace): the hash definition has to change for AC #5.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Sreality detail responses are scrubbed of the seller's contact data (result.user, result.premise.web_url, result.premise.web_visible_url) in SrealityClient.fetchDetail, before hashing and storing. payload_hash is now SHA-256 of the payload's jsonb text form computed by Postgres, so migration V10 could scrub every existing snapshot and recompute the same hash: an unchanged listing is not re-snapshotted on the next run. decision-8 records the change to decision-2. README says snapshots are stored without contact data. Verified by mvn verify and on the local DB. Follow-ups: snapshots that differed only in contact data are now duplicates (left as is); disk space is reclaimed only by VACUUM FULL listing_snapshot.
<!-- SECTION:FINAL_SUMMARY:END -->
