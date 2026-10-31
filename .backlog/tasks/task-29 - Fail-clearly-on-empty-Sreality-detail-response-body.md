---
id: TASK-29
title: Fail clearly on empty Sreality detail response body
status: Done
assignee:
  - '@claude'
created_date: '2026-10-31T13:35:00Z'
updated_date: '2026-10-31T20:24:00Z'
labels:
  - scraping
milestone: m-2
dependencies: []
ordinal: 29000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SrealityClient.fetchEstateDetail (TASK-10) returns RestClient's body(String.class), which is null when a 200 response has no body. SrealityEstateFetcher (TASK-12) then passes null to ListingSnapshotService.recordFetch, which throws a NullPointerException while hashing the payload. Same class of bug as the one fixed in countEstates during TASK-13 (there an IllegalStateException is thrown when pagination.total is missing). Make the detail call fail with a clear exception instead, and decide whether an empty/blank body should be treated the same.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A 200 detail response with an empty body raises a clear exception from SrealityClient instead of returning null
- [x] #2 Nothing is recorded in listing/listing_snapshot for such a response
- [x] #3 A WireMock test covers the empty-body case
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Folded into TASK-15's change (user decision): fetchEstateDetail throws IllegalStateException when a 200 body is null or blank; WireMock tests in SrealityClientTest (empty + blank) and SrealityEstateFetcherTest (nothing recorded).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Done together with TASK-15 (user decision), since TASK-15's Optional.of(response.bodyTo(...)) made the null visible as an IDE warning. fetchEstateDetail throws IllegalStateException('Sreality returned an empty body for estate <id>') for a null or blank 200 body; blank treated like empty per user. Not SrealityApiException, since the status was 200 (same as countEstates' missing-total check). Tests: SrealityClientTest.emptyOrBlankDetailBodyFailsInsteadOfReturningNothing ('' and whitespace), SrealityEstateFetcherTest.emptyBodyRecordsNothing. mvn verify: 46 tests, BUILD SUCCESS.

Finalized via /finish-task together with TASK-15: mvn verify green (46 tests).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
A 200 detail response with an empty or blank body now fails in SrealityClient.fetchEstateDetail with IllegalStateException naming the estate, instead of a bare NullPointerException; nothing is recorded in listing/listing_snapshot. Done alongside TASK-15. Tests cover empty and whitespace-only bodies (client) and that the fetcher records nothing. mvn verify green, 46 tests.
<!-- SECTION:FINAL_SUMMARY:END -->
