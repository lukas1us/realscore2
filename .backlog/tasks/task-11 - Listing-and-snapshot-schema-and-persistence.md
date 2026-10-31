---
id: TASK-11
title: Listing and snapshot schema and persistence
status: Done
assignee:
  - '@lkrasa'
created_date: '2026-10-30T23:42:00Z'
updated_date: '2026-10-31T12:47:00Z'
labels:
  - listing
milestone: m-2
dependencies:
  - TASK-9
ordinal: 11000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implements the 'listing identity' and 'store raw first' decisions as actual tables and repositories: a listing row keyed by (source, external_id) with seen/removed timestamps, and an append-only snapshot table holding the raw JSONB payload, written only when it changes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A Flyway migration creates listing (UNIQUE(source, external_id), first_seen_at, last_seen_at, removed_at) and listing_snapshot (JSONB payload, payload hash, timestamp, FK to listing)
- [x] #2 Saving the same payload twice for the same listing writes exactly one snapshot row
- [x] #3 Saving a changed payload for the same listing writes a second snapshot row
- [x] #4 A Testcontainers-backed test proves both cases above against real Postgres
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add V2__create_listing_and_listing_snapshot.sql (reviewed and approved) creating listing and listing_snapshot tables.
2. Add internal JPA entities com.realscore.listing.internal.Listing and ListingSnapshot, plus Spring Data repositories ListingRepository and ListingSnapshotRepository.
3. Add public service com.realscore.listing.ListingSnapshotService with recordFetch(source, externalId, payload): finds-or-creates the Listing row (updating last_seen_at), computes a SHA-256 hex hash of the payload, and only inserts a new ListingSnapshot when the hash differs from the latest existing snapshot for that listing.
4. Add a Testcontainers-backed test (com.realscore.listing.ListingSnapshotServiceTest, @SpringBootTest + @Import(PostgresContainerConfig.class)) proving: (a) recordFetch twice with the same payload yields exactly one snapshot row, (b) recordFetch with a changed payload yields a second snapshot row.
5. Run mvn verify to confirm everything passes against real Postgres with ddl-auto=validate.
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added V2__create_listing_and_listing_snapshot.sql (listing with UNIQUE(source, external_id) + first/last_seen_at/removed_at; listing_snapshot with JSONB payload, payload_hash, fetched_at, FK to listing). Added internal JPA entities/repositories (com.realscore.listing.internal) and a public ListingSnapshotService.recordFetch(source, externalId, payload) that finds-or-creates the Listing (bumping last_seen_at) and only inserts a new snapshot when its SHA-256 hash differs from the latest one for that listing (no new dependency needed for hashing or JSONB mapping - plain JDK MessageDigest/HexFormat and Hibernate 6's native @JdbcTypeCode(SqlTypes.JSON)). ListingSnapshotServiceTest (Testcontainers Postgres) proves: same payload twice -> 1 snapshot row; changed payload -> 2 snapshot rows. mvn verify passes (8 tests total, including ModularityTests with no boundary violations).
<!-- SECTION:FINAL_SUMMARY:END -->
