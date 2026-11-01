---
id: TASK-38
title: Verify unseen listings before marking them removed
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T17:32:00Z'
updated_date: '2026-11-01T19:59:00Z'
labels:
  - scraping
milestone: m-7
dependencies: []
type: bug
ordinal: 48000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A listing that drops out of the search results is marked removed, but that also happens to live listings, e.g. when the price rises above `realscore.scrape.price-cap` or the category changes. The removal data and the price history are then wrong.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 After a successful run, the detail is fetched for every listing that run did not see
- [x] #2 Detail 404/410 sets removed_at
- [x] #3 Detail 200 records the snapshot and keeps the listing active
- [x] #4 A test covers a listing whose price rises above the price cap: it stays active and its new price is recorded
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. SrealityClient.fetchEstateDetail returns a small sealed result instead of Optional: Found(body) for 200, Gone for 404 and 410, Forbidden for 403; other statuses still throw SrealityApiException. SrealityEstateFetcher/SrealityBulkCollector keep today's behaviour (Gone and Forbidden = skipped); 410 thus becomes a skip instead of a failure in the bulk path.
2. listing module: recordPresence only marks the seen IDs (last_seen_at, clears removed_at) and returns reappeared; markRemovedIfNotSeenSince goes. New ListingSnapshotService.findActiveExternalIdsNotSeenSince(source, since) and markRemoved(source, externalIds, at).
3. New scraping/internal SrealityRemovalCheck: for each active listing the run did not see, fetch the detail. Found -> recordFetch (snapshot if changed, listing stays active, last_seen_at moves); Gone -> marked removed; Forbidden or any other failure -> left active and checked again next run (logged, counted as failed).
4. SrealityScrapeJob: after a successful collection that found IDs, recordPresence then SrealityRemovalCheck; scrape_run removed_count = Gone count, updated_count includes changed snapshots from the check, failed_count includes check failures. No migration.
5. Tests: client maps 404/410/403; job test: unseen listing with detail 404 and with 410 -> removed; unseen with detail 200 at a price above the cap -> stays active, new snapshot and price point; unseen with 500 -> stays active; existing presence tests adjusted (the reappearing listing is stubbed 404 for run 2).
6. README: scraping/listing steps describe the detail check before removal, and its cost (every unseen active listing is fetched each run).
7. mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Decided with the user (2026-11-01): option (b), implement against stubbed 404/410; what Sreality really answers for an ended listing is still unverified and must be checked on a real run (if it is 200, nothing gets removed and unseen listings are fetched every run). 403 leaves the listing active.

Implemented as planned. 'Every listing the run did not see' means every active one: listings already marked removed are not re-checked. 410 is now a skip (not a failure) in the bulk path too. PresenceResult removed; recordPresence returns the reappeared count. Tests: SrealityClientTest (403 Forbidden, 404/410 Gone, 400 still throws), ListingSnapshotServiceTest (recordPresence no longer removes, findActiveExternalIdsNotSeenSince, markRemoved), SrealityScrapeJobTest (404 and 410 removed, 403 and 500 kept and counted as errors, unseen listing above the price cap kept with its new price point 5,900,000 and updated_count 1). mvn verify: 190 tests, 0 failures. Not verified against the live API: what Sreality returns for an ended listing.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
A listing that drops out of the search results is no longer marked removed on that alone. After each successful run that found IDs, SrealityRemovalCheck fetches the detail of every active listing the run did not see: 404/410 marks it removed, 200 records the snapshot (so the price history gets e.g. a price that rose above the cap) and keeps it active, 403 or any other failure leaves it active to be checked next run. SrealityClient.fetchDetail returns Found/Gone/Forbidden; the listing module now has findActiveExternalIdsNotSeenSince and markRemoved, and recordPresence only marks seen listings. scrape_run: removed_count = gone listings, updated_count and error_count include the check. README describes the check and its cost. Risk: still unverified what Sreality answers for an ended listing; if it is 200, nothing is ever removed and every unseen listing is fetched on every run, so check the first real run's removed_count and logs.
<!-- SECTION:FINAL_SUMMARY:END -->
