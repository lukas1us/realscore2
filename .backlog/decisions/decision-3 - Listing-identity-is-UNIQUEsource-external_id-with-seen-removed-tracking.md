---
id: decision-3
title: Listing identity is UNIQUE(source, external_id) with seen/removed tracking
date: '2026-10-30T23:39:00Z'
status: accepted
---
## Context

The app needs to track listings across repeated scrapes over time — price changes, and listings disappearing from the market — rather than just holding a snapshot of "what's currently listed." It also needs to support more than one source portal eventually (Bezrealitky, iDNES are noted as later work), so identity can't be scoped to a single source's ID space.

## Decision

A listing's identity is `UNIQUE(source, external_id)`, where `external_id` is the source portal's own ID (e.g. the Sreality estate ID) and `source` disambiguates portals. Each listing tracks `first_seen_at`, `last_seen_at`, and `removed_at`.

## Consequences

Price history and delisting become directly queryable from `listing` + `listing_snapshot` without extra bookkeeping. Re-scraping a known listing is an update, not a duplicate insert, as long as `(source, external_id)` is used consistently as the lookup key everywhere — including by the future `parsing`/`analysis` modules, which must never assume IDs are globally unique without the source qualifier.

