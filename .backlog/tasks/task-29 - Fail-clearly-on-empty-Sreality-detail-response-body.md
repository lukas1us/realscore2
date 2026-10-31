---
id: TASK-29
title: Fail clearly on empty Sreality detail response body
status: To Do
assignee: []
created_date: '2026-10-31T13:35:00Z'
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
- [ ] #1 A 200 detail response with an empty body raises a clear exception from SrealityClient instead of returning null
- [ ] #2 Nothing is recorded in listing/listing_snapshot for such a response
- [ ] #3 A WireMock test covers the empty-body case
<!-- AC:END -->
