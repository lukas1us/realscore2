---
id: decision-2
title: Store raw snapshots first, parse separately and idempotently
date: '2026-10-30T23:39:00Z'
status: accepted
---
## Context

Sreality's API (see the "Sreality API" doc) is undocumented and was learned by trial and error; the old app's parsing logic changed repeatedly as new gotchas were found (price field choice, locality string formats, etc.). If parsing happens at scrape time and only the parsed result is kept, every parsing bug permanently loses data for listings scraped before the fix — there is no way to reprocess them.

## Decision

Every fetched API response is stored as-is as JSONB in `listing_snapshot`, including a payload hash, written only when the payload actually changes since the last snapshot. Normalisation into structured fields is a separate step (the `parsing` module) that reads snapshots and is idempotent and safely re-runnable at any time. Scraping itself never computes scores or derived fields.

## Consequences

A parsing bug or a new field requirement can be fixed by re-running parsing over existing snapshots, with no new scraping needed — this is exactly what M5's "re-run parsing" task provides. The tradeoff is more storage (full raw payloads, deduplicated only by change) and an extra step between fetching and having usable data, which is acceptable for a periodic batch-style app.

