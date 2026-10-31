---
id: TASK-11
title: Listing and snapshot schema and persistence
status: To Do
assignee: []
created_date: '2026-10-30T23:42:00Z'
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
- [ ] #1 A Flyway migration creates listing (UNIQUE(source, external_id), first_seen_at, last_seen_at, removed_at) and listing_snapshot (JSONB payload, payload hash, timestamp, FK to listing)
- [ ] #2 Saving the same payload twice for the same listing writes exactly one snapshot row
- [ ] #3 Saving a changed payload for the same listing writes a second snapshot row
- [ ] #4 A Testcontainers-backed test proves both cases above against real Postgres
<!-- AC:END -->
