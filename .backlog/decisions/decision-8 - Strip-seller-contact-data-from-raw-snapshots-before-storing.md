---
id: decision-8
title: Strip seller contact data from raw snapshots before storing
date: '2026-11-01T20:03:00Z'
status: accepted
---
## Context

decision-2 stores every fetched API response as-is. Sreality detail responses include the broker's name, email and phones (`result.user`) and the agency's web URLs (`result.premise.web_url`, `result.premise.web_visible_url`). That is personal data the app never uses (TASK-40).

## Decision

This amends decision-2. Before a Sreality payload is hashed and stored, `result.user`, `result.premise.web_url` and `result.premise.web_visible_url` are removed (`SrealityPayloadScrubber`, applied in `SrealityClient.fetchDetail`). Everything else is still stored as received. `payload_hash` is SHA-256 of the payload's jsonb text form, computed by Postgres, so the same hash can be computed in SQL.

## Consequences

No contact data is kept; migration V10 scrubbed the existing snapshots and recomputed their hashes, so the next run does not re-snapshot unchanged listings. Those fields can never be re-parsed later. Whitespace or key-order differences in a response no longer count as a change. Snapshots that differed only in contact data are now duplicates in the history; they are left as they are.

