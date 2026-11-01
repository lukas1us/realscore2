---
id: doc-1
title: Sreality API
type: specification
created_date: '2026-10-30T23:36:00Z'
updated_date: '2026-11-01T20:51:00Z'
---
# Sreality API

Reverse-engineered knowledge, originally extracted from the old `realscoreCZ` Python app (itself almost entirely AI-generated, never checked against an official API doc). On 2026-10-31, the endpoint that document originally described (`/api/cs/v2/estates`) was found to be **fully retired** — confirmed dead via direct live testing, not just inferred from stale code. This document now describes the **v1** API, verified directly against live responses captured that same day. Facts are marked **CONFIRMED** (checked against a live v1 response), **[UNVERIFIED]** (inherited from the old app's code/comments, or otherwise not independently re-checked), or **NEW** (not present in the old doc at all).

This remains an undocumented internal API. It has now demonstrably changed its entire base path and response shape with zero public notice once already — treat everything here as provisional, and re-verify before any milestone that depends heavily on a specific field or endpoint.

## Endpoints

- **Detail**: `GET https://www.sreality.cz/api/v1/estates/{estate_id}` → `200`, JSON `{"result": {...}}`. CONFIRMED live.
- **Search**: `GET https://www.sreality.cz/api/v1/estates/search?<params>` → `200`, JSON (shape below). CONFIRMED live.
- The old `/api/cs/v2/estates` path (both the detail and search forms) and a probed `/api/cs/v3/estates` are **fully retired**: confirmed via direct requests to return a generic nginx 404 — for a real, currently-live listing ID, for a trivial ID (`1`), and for the ID this document was originally asked to capture. Not a per-listing issue; the whole path is gone.
- The estate ID is still the trailing numeric segment of a Sreality detail URL. CONFIRMED (same ID worked once the base path was corrected).
- HTTP headers used successfully in testing: a desktop Chrome `User-Agent`, `Accept: application/json`, `Referer: https://www.sreality.cz/`. **[UNVERIFIED]** whether strictly required — never tested their absence.

## Search query parameters (v1)

Confirmed live from a real `estates/search` URL (captured from the site's own "search settings" / saved-search feature), cross-checked against that response's own `meta_description` text, which spells out the applied filters in Czech:

| Param | Meaning | Values | Status |
|---|---|---|---|
| `category_main_cb` | property type | `1`=byty confirmed; `2`=domy, `3`=pozemky, `4`=komerční, `5`=ostatní inherited from the old doc, not re-verified | PARTIALLY CONFIRMED |
| `locality_region_id` | region filter | comma-separated list of region ids, e.g. `2,11,7,6` (same 1–14 table as before — see below) | CONFIRMED |
| `multilocality_type_or` | OR vs AND across `locality_region_id`'s values | `true` / `false` | **NEW**, CONFIRMED |
| `price_to` | price ceiling (CZK) | integer | CONFIRMED (re-checked 2026-10-31, TASK-14: Praha sale flats `price_to=5000000` → 347 vs 6047 uncapped, title "…do 5 000 000 Kč"). `price_from` presumably symmetric — **[UNVERIFIED]**, not in the sample URL |
| `category_type_cb` | transaction type | `1`=prodej, `2`=pronájem | CONFIRMED 2026-10-31 (TASK-14): Praha flats `=1` → 6047 "Byty na prodej Praha", `=2` → 3517 "Byty k pronájmu Praha"; results carry `category_type_cb.value` matching the filter |
| `category_sub_cb` | disposition filter | codes from the disposition table below | CONFIRMED 2026-10-31 (TASK-14): `category_sub_cb=4` → title "Byty 2+kk na prodej Praha", 1860 results, every sampled result had `category_sub_cb.value=4` |
| `ownership` | ownership filter | `1`=osobní CONFIRMED (resolves the old doc's `ownership`/`ownership_type_cb` ambiguity — **this is the real field name**); `2`=družstevní, `3`=státní/obecní **[UNVERIFIED]**, inherited from old code comments only |
| `building_type` | construction type — **not** disposition | comma-separated list, see table below | **NEW**, CONFIRMED |
| `limit` / `offset` | pagination | `limit=0` returns zero results with just the total count — a clean replacement for the old `per_page=1` probe hack. No cap observed: `limit=1000` returned 1000 results, `offset=6000&limit=100` returned the last 47 of 6047 (TASK-14) | CONFIRMED |

`category_type_cb` and `category_sub_cb` were not in the one real captured URL, but were verified directly as v1 search params on 2026-10-31 (TASK-14) — see the table.

**[UNVERIFIED]** `usable_area_from/to` have not been re-tested against v1 — carried over from the old doc as hypotheses only. The old ~1000-result cap was re-tested and not observed on v1 — see "The ~1000-result cap and bucketing workaround" below.

### Region IDs (`locality_region_id`) — re-confirmed

Same table as before; re-confirmed 2026-10-31 by decoding a live search response's `meta_description` text (which named "Královéhradecký kraj, Pardubický kraj, Plzeňský kraj, Středočeský kraj" for `locality_region_id=2,11,7,6`) against this table:

| ID | Region |
|---|---|
| 1 | Jihočeský kraj |
| 2 | Plzeňský kraj |
| 3 | Karlovarský kraj |
| 4 | Ústecký kraj |
| 5 | Liberecký kraj |
| 6 | Královéhradecký kraj |
| 7 | Pardubický kraj |
| 8 | Olomoucký kraj |
| 9 | Zlínský kraj |
| 10 | Praha |
| 11 | Středočeský kraj |
| 12 | Moravskoslezský kraj |
| 13 | Kraj Vysočina |
| 14 | Jihomoravský kraj |

The old app's slug→region-ID map for city/district names is not reproduced here; re-derive from Czech administrative regions if needed.

### Disposition codes (`category_sub_cb`) — re-confirmed on a live detail response

Same table as before; re-confirmed 2026-10-31 — a live listing titled "Prodej bytu **1+1** 37 m²" had `category_sub_cb: {"name": "1+1", "value": 3}`, matching this table exactly:

| Code | Disposition |
|---|---|
| 2 | 1+kk |
| 3 | 1+1 |
| 4 | 2+kk |
| 5 | 2+1 |
| 6 | 3+kk |
| 7 | 3+1 |
| 8 | 4+kk |
| 9 | 4+1 |
| 10 | 5+kk |
| 11 | 5+1 |
| 12 | 6+ |

(Code `1` still unused, as before.)

### Construction type codes (`building_type`) — NEW, not in the old doc at all

Confirmed via the same live search's `meta_description` decode, cross-checked against a live detail response (`building_type: {"name": "Cihlová", "value": 2}`):

| Code | Type |
|---|---|
| 1 | dřevostavba |
| 2 | cihlová |
| 3 | kamenná |
| 4 | montovaná |
| 6 | skeletová |
| 7 | smíšená |
| 8 | modulární |

Code `5` was not observed in the sample — **[UNVERIFIED]** whether it's unused (like disposition code `1`) or just absent from this particular query.

## Search response shape (v1)

```
{
  "status_code": 200,
  "status_message": "OK",
  "search_title": "<string>",
  "meta_title": "<string>",
  "meta_description": "<string>",
  "pagination": { "limit": <int>, "offset": <int>, "total": <int> },
  "results": [ ... ]
}
```

`pagination.total` replaces the old `result_size`; `limit`/`offset` replace `per_page`/`page`.

CONFIRMED 2026-10-31 (TASK-20) — captured a real search with `limit=50`. Each item in `results` has: `advert_images`, `advert_images_all`, `advert_name`, `category_main_cb`, `category_sub_cb`, `category_type_cb`, `discount_show`, `has_matterport_url`, `has_video`, `hash_id`, `locality`, several `poi_*_distance` fields, `premise`, `premise_id`, `premise_logo`, `price`, `price_currency_cb`, `price_czk`, `price_czk_m2`, `price_summary`, `price_summary_czk`, `price_summary_unit_cb`, `price_unit_cb`, `user_id` — a smaller, list-view-oriented subset of the detail response's fields, not a different shape. `price_czk` was a plain number in every item checked (both apartment and commercial samples) — the old doc's "bare number vs. object" gotcha was not observed on v1.

## Detail response shape (v1)

Confirmed live — see the scrubbed fixture at `src/test/resources/fixtures/sreality/detail_success.json`. Top-level shape is `{"result": {...}}`.

**The biggest change from the old doc: there is no `items` list at all.** Every fact the old doc extracted from `items` via multi-name fallback priority is now a direct, named field:

- `usable_area`, `floor_area`, `floor_number`, `floors` — plain numbers.
- `category_main_cb`, `category_sub_cb`, `category_type_cb`, `building_type`, `ownership`, `elevator`, `building_condition`, `energy_efficiency_rating_cb`, `flat_class`, `furnished`, `easy_access` — each a self-describing `{"name": "<human Czech label>", "value": <code>}` object. Only `value` matters for comparison/storage; `name` is a ready-made display label.
- `heating_set`, `heating_source_set`, `heating_element_set`, `gas_set`, `gully_set`, `electricity_set` — each a **list** of `{"name", "value"}` objects (a field can carry more than one value).
- `locality` — a structured object (`city`, `city_seo_name`, `citypart`, `district`, `district_id`, `region`, `region_id`, `street`, `street_id`, `housenumber`, `streetnumber`, `gps_lat`, `gps_lon`, `ward`, `quarter`, `zip`, plus `*_seo_name` variants and a `geometry` block). This completely replaces the old doc's whole "disambiguate a comma-separated string by part count" logic — city/district/region are already separate fields.
- `price_summary_czk` (or `price_summary`) is the **authoritative total price** — CONFIRMED 2026-10-31 (TASK-20). `price_czk`/`price` can instead be a *per-unit* rate: on a commercial rental, a listing had `price_czk=450` with `price_unit_cb={"name": "za m²/měsíc", ...}` (per m²/month) while `price_summary_czk=433800` with `price_summary_unit_cb={"name": "za měsíc", ...}` (per month) — `450 × usable_area(964) = 433800`, confirming `price_summary_czk` is the one to use regardless of unit, never `price_czk` directly. This supersedes the old doc's "prefer the 'Celková cena' items entry" rule, which doesn't carry over since there's no `items` list at all on v1.
- **"Price on request" (old doc's "Cena na vyžádání")** — CONFIRMED 2026-10-31 (TASK-20): on such listings (found in the commercial category), `price_czk` **and** `price_summary_czk` are both `0`, together. There's no dedicated flag — `price_flag_negotiation_cb` was `false` on every such listing checked, and `price_note` was empty/null. Treat `price_summary_czk == 0` (or missing) as "no price," not an error. NEW 2026-11-01 (scrape run 2): a flat for sale switched to price on request showed the same zeros (`price`, `price_czk`, `price_summary`, `price_summary_czk` all `0.0`, `price_czk_m2` `0`), but this time with `price_note` = `"cena u makléře"`, so `price_note` is not always empty on such listings.
- `advert_description` — free text, same role as the old doc's `text`/`description`. The one sample captured had no embedded contact details (just "contact us"), but **[UNVERIFIED]** whether that generalizes.
- `user` — the seller/broker contact block (`user_name`, `user_email`, `user_phones`, `image`, `user_id`, `broker_rating`, `broker_review_count`). **Contains PII.** Must be scrubbed from any committed fixture (done in the current ones). Not stored in the DB either: since TASK-40 (decision-8) the app removes the whole `user` block before hashing and storing a snapshot, and migration V10 scrubbed the existing snapshots.
- `premise` — the listing agency's info (`name`, `company`, `locality`, `web_url`/`web_visible_url`, `logo`, review data). `web_url`/`web_visible_url` can itself leak the broker's name (observed: a personal-name domain) — treat as PII-adjacent too. Since TASK-40 (decision-8) these two fields are removed before storing, like `user`; the rest of `premise` is kept.
- `advert_images` — list of image metadata (`id`, `url`, `width`, `height`, `order`, `kind`, `ai_edited`, `alt`). No PII observed.
- `labels` / `labels_extended` — nearby points of interest (shops, doctors, restaurants, transit), sourced from mapy.cz/firmy.cz. Not personal data, just large.
- `hash_id` — equals the requested estate ID.

**The old doc's whole field-by-field priority-fallback extraction methodology (preferring "Celková cena" over "Cena", multi-name `items` fallbacks for size/year/energy class/elevator/ownership/service charge, regex-extracting disposition from the title) is obsolete for v1** — every one of those facts now has a direct field. It is intentionally not carried forward into this rewrite; re-add equivalent fallback logic only if a future capture shows a field is sometimes missing and needs a substitute source.

### Per-request volatility — checked, none found

`ListingSnapshotService` (TASK-11) decides whether a listing changed by hashing the entire detail payload. Until TASK-40 the hash was SHA-256 of the raw response string; since TASK-40 (decision-8) it is SHA-256 of the payload's jsonb text form, computed by Postgres after the seller's contact data is removed, so whitespace and key order no longer count as a change. Either way, if the API returns any field that changes on every request regardless of real listing changes (view counters, request timestamps, token-bearing image URLs, the `labels`/`labels_extended` POI blocks being recomputed, etc.), that dedup would be defeated — every fetch would look "changed."

Checked 2026-10-31 (TASK-28): fetched estate `2494705740` twice via `GET /api/v1/estates/{id}`, 5 minutes apart, same headers the client uses. The two raw response bodies were **byte-for-byte identical** (same SHA-256 digest). No volatile fields of any kind were observed — not in `labels`/`labels_extended`, not in image URLs, nothing.

**[UNVERIFIED]** This was checked on one estate, over one 5-minute gap. It doesn't rule out volatility over a longer gap (hours/days), under different request conditions (e.g. a different session/cookie state), or on a different estate. If a future capture ever disagrees with this finding, revisit `ListingSnapshotService`'s whole-payload-hash approach then — no change was needed as of this check (the TASK-40 change was about contact data, not volatility).

## The ~1000-result cap and bucketing workaround

**Re-tested 2026-10-31 (TASK-14): no cap observed on v1.** For Praha sale flats (`total=6047`), `limit=1000&offset=0` returned 1000 results, `limit=100&offset=5000` returned 100, and `limit=100&offset=6000` returned the remaining 47 — paging past 1000 works, and `pagination.total` matched what was reachable. **[UNVERIFIED]** whether an undocumented cap exists above ~6000 or for larger `limit` values (not tested), or whether this changes again.

The bucketing below is implemented anyway (`SrealityIdCollector`, TASK-14) as a defensive measure: smaller queries are less exposed to listings shifting during long paging, and it guards against a cap returning. Threshold and page size are configurable (`realscore.sreality.bucket-threshold`, default 950; `realscore.sreality.page-size`, default 100). The old app's `BUCKET_LIMIT=950`, `per_page=20` and ~1000 cap are historical.

The old app's workaround, for a *sale apartments, price-capped* scan:

1. For each of the 14 regions, send a cheap probe with the target filters and `locality_region_id=<region>` and read the total count — `limit=0` on v1, replacing the old `per_page=1, page=1` probe.
2. If a region's count is at or under a safety threshold, that region is one collection bucket — paginate it directly.
3. If a region's count exceeds the threshold, subdivide further: probe every disposition code (`category_sub_cb`) within that region, and treat each non-empty `(region, disposition)` pair as its own bucket.
4. Collect IDs from every bucket by paginating with a fixed page size until exhausted.
5. Dedupe all collected IDs across all buckets with a set.

**Known residual gap, inherited on purpose**: if a single `(region, disposition)` bucket itself still exceeds the threshold, the old app only logged a warning and did not subdivide further (e.g. by price band). Deferred unless it proves necessary.

### Ended listings and detail status codes — CONFIRMED 2026-11-01

- **An ended listing answers the detail request with HTTP 404.** CONFIRMED 2026-11-01 (TASK-38, scrape run 2): of the 55 active listings that dropped out of that run's search results, 54 answered `404` to `GET /api/v1/estates/{id}`. None answered `410` or `403`, and none failed otherwise. The app treats `404` and `410` as gone (marks the listing removed) and `403` as no proof either way (hidden, or the request was refused), leaving the listing active. **[UNVERIFIED]** whether Sreality ever sends `410` or `403` for a single estate at all.
- **Dropping out of the search results is not proof that a listing is gone.** CONFIRMED 2026-11-01: the 55th listing (`3791487052`, a 3+kk for sale) still answered `200`. Its seller had switched it from 4,295,000 CZK to price on request (all price fields `0`, see above), which took it out of the search filtered by `price_to`. A price rise above the cap or a category change would do the same. Only the detail request tells a removed listing from one the search no longer matches.

## Rate limiting and retries

- Detail-fetch retries: up to `max_retries` (old default `3`) attempts with exponential backoff (`2^attempt` seconds) on **retriable** failures — HTTP 429 or connection/transient errors.
- **Non-retriable**: HTTP 403 or 404 → skip immediately, no retry.
- The new app (TASK-15, TASK-38): HTTP 429 and 5xx are retried with backoff; 403, 404 and 410 are not retried. In the bulk path they are all a skip; for a listing the run did not see, 404 and 410 mark it removed and 403 leaves it active (see "Ended listings and detail status codes").
- A fixed polite delay (old default `0.5s`) was slept after every request attempt regardless of outcome, to cap the steady-state request rate.
- This is about client behavior, not the specific endpoint shape, so it's unaffected by the v1 migration. Nothing here implies the new Java app needs the same concurrency shapes as the old Python app, only that *some* shared backoff/skip/delay policy is needed (tracked as its own task in M3).

## Rental benchmark lookups

Used to estimate achievable rent for a (city, disposition) pair, via the same `/api/v1/estates/search` endpoint (path updated from the retired v2 search) with `category_main_cb=1` (byty) and `locality_region_id=<city's region>` when the city maps to one of the 14 regions (otherwise nationwide). `category_type_cb=2` (pronájem) and `category_sub_cb=<disposition>` are confirmed v1 search params (TASK-14, see above). **[UNVERIFIED]** the rest of this approach has not been re-tested on v1.

Two gotchas carried over from the old doc, **not yet re-verified against v1**:
- `price_czk` on each result estate may still need defensive handling (the old API sometimes returned a bare number vs. an object) — unknown whether v1's `results` items have the same quirk; the `results` item shape is now confirmed (see "Search response shape"), and `price_czk` was a plain number in every item checked.
- `listing_count` semantics (region-wide total vs. page-1 sample count for cities without a region match) — same caveat.

## Out of scope for v1 (of the roadmap, not the API version)

- **Bezrealitky** and **iDNES**: both HTML-scraped, no JSON API. Left as a "Later" backlog task.
- Scoring, ČSÚ demographic/economic data, benchmark tables, and the Streamlit UI from the old app: out of scope until the "Later" analysis/scoring/UI tasks.

## Biggest open risks

- **This API has already changed its entire base path and response shape once, with zero public notice**, between when the old Python app was written and when this document was re-verified live (2026-10-31). It is an undocumented internal API and can change again at any time. Re-verify before any milestone that depends heavily on a specific field or endpoint — don't assume anything in this document stays true indefinitely.
- **[UNVERIFIED]** No mention of Sreality's Terms of Service or `robots.txt` was found anywhere in the old codebase, and none was checked here either — scraping this internal API's legal status under Sreality's ToS remains unaddressed.
