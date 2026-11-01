---
id: TASK-30
title: Replace hand-written boilerplate with Lombok in existing code
status: Done
assignee:
  - '@claude'
created_date: '2026-10-31T13:35:00Z'
updated_date: '2026-11-01T13:31:00Z'
labels:
  - infra
dependencies: []
ordinal: 30000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
CLAUDE.md now requires Lombok for boilerplate (getters/setters, constructors, builders). Code written before that rule still hand-writes it, e.g. the dependency-injection constructors of SrealityEstateFetcher (scraping.internal) and ListingSnapshotService (listing), and any other hand-written accessors/constructors in main code. Convert them (e.g. @RequiredArgsConstructor, keeping current constructor visibility where it matters for module boundaries) without changing behaviour. SrealityApiException was already converted in TASK-13; constructors that contain logic (not just field assignment) stay hand-written.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Main code has no hand-written constructors/accessors that only assign or return fields; Lombok is used instead
- [x] #2 Behaviour and public API names are unchanged; mvn verify passes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. ScrapeRunService: replace the package-private DI constructor with @RequiredArgsConstructor(access = AccessLevel.PACKAGE).
2. SrealityEstateFetcher: same, @RequiredArgsConstructor(access = AccessLevel.PACKAGE).
3. MedianBenchmarks: private no-op constructor -> @NoArgsConstructor(access = AccessLevel.PRIVATE).
4. Keep hand-written (logic or no Lombok equivalent): SrealityClient (builds RestClient), SrealityApiException (message, overload chaining), ScrapeRun (status default), Listing (derives lastSeenAt/lastFetchedAt), ListingSnapshot and ListingDetail (JPA entities assigning all fields except the generated id; @AllArgsConstructor would add id to the public API, @RequiredArgsConstructor+@NonNull would add null checks = behaviour change).
5. No hand-written accessors found in main code. mvn verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Waited for TASK-32 (adc6974) to land, as it touched ScrapeRunService. Kept hand-written on purpose: SrealityClient, SrealityApiException, ScrapeRun, Listing (constructors with logic) and ListingSnapshot, ListingDetail (JPA entities assigning all fields but the generated id; @AllArgsConstructor would expose id, @RequiredArgsConstructor+@NonNull would add null checks). No hand-written accessors in main code. README unaffected (no user-facing change).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Replaced the remaining boilerplate constructors in main code with Lombok: ScrapeRunService and SrealityEstateFetcher use @RequiredArgsConstructor(access = PACKAGE), keeping their package-private constructors; MedianBenchmarks uses @NoArgsConstructor(access = PRIVATE). Constructors with logic and the JPA entity constructors stay hand-written (see notes). No behaviour or API change; mvn verify passes (100 tests).
<!-- SECTION:FINAL_SUMMARY:END -->
