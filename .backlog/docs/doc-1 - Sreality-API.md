---
id: doc-1
title: Sreality API
type: specification
created_date: '2026-10-30T23:36:00Z'
updated_date: '2026-10-30T23:38:00Z'
---
# Sreality API

Reverse-engineered knowledge extracted from the old `realscoreCZ` Python app (itself almost entirely AI-generated, never checked against an official API doc). This is undocumented internal API — everything here comes from reading `backend/scrapers/sreality.py`, `sreality_search.py`, `constants.py`, `jobs/full_market_scan.py`, `jobs/rent_market_scan.py`, and the `fix:` commits in that repo's git history. Claims that could not be independently verified (no official docs exist) are marked **[UNVERIFIED]**.

## Endpoints

- **Detail**: `GET https://www.sreality.cz/api/cs/v2/estates/{estate_id}` — one listing's full JSON.
- **Search / list**: `GET https://www.sreality.cz/api/cs/v2/estates` — paginated search results, query params below.

The estate ID is the trailing numeric segment of a Sreality detail URL, e.g. `.../Praha/.../12345678` → `12345678`.

HTTP headers sent on every request by the old app: a desktop Chrome `User-Agent`, `Accept: application/json`, `Referer: https://www.sreality.cz/`. **[UNVERIFIED]** whether these are strictly required — the old app always sent them but never tested their absence.

## Search query parameters

| Param | Meaning | Values |
|---|---|---|
| `category_main_cb` | **property type** | `1`=byty, `2`=domy, `3`=pozemky, `4`=komerční, `5`=ostatní |
| `category_type_cb` | **transaction type** | `1`=prodej (sale), `2`=pronájem (rent) |
| `category_sub_cb` | disposition | see table below; API accepts this repeated for multiple values |
| `locality_region_id` | region pre-filter | `1`–`14`, see table below |
| `price_from` / `price_to` | price range (CZK) | integer |
| `usable_area_from` / `usable_area_to` | floor area range (m²) | integer |
| `ownership_type_cb` | ownership filter | `1`=osobní, `2`=družstevní |
| `per_page` / `page` | pagination | `per_page` observed used at `1` (cheap probe), `20` (search), `50` (max per old code's assumption, see cap section) |

**[UNVERIFIED] Inconsistency found in the old code**: `full_market_scan.py`'s `base_params` also sets a param literally named `"ownership"` (not `ownership_type_cb`) to `1`, with a comment "1=osobní, 2=družstevní, 3=státní/obecní". It's unclear whether `ownership` and `ownership_type_cb` are the same underlying API filter under two names, two different filters, or whether one of them is simply dead/non-functional code. Verify directly against the live API before relying on either.

**[UNVERIFIED] Possible bug in old code, never fixed**: `sreality.py`'s `_canonical_url()` (reconstructs a pretty web URL from the *detail* endpoint's embedded `seo` block, not from search params) uses the **opposite** axis labeling from the table above: it reads `seo["category_main_cb"]` as the *transaction* type (`1`=prodej, `2`=pronájem) and `seo["category_type_cb"]` as the *property* type (`1`=byt, `2`=dům, `3`=pozemek, `4`=komerční, `5`=ostatní). This was never touched by the commit that fixed the search-API axis mix-up (`35db3f3`, see below), which only patched `sreality_search.py` and `scrape_rental_estimates`. Either the detail endpoint's `seo` block genuinely uses different semantics than the search endpoint's query params, or `_canonical_url` has the same bug and it went unnoticed because it only affects a cosmetic display URL, not an actual API call. **Re-verify against a live detail response before porting any canonical-URL logic.**

### Region IDs (`locality_region_id`)

Confirmed via the old code's own comments (claimed "verified via API" there, not re-verified here):

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

The old app additionally kept a large slug→region-ID map (city/district slugs like `"liberec"`, `"ostrava"` → their region ID) for converting a human city name into the right `locality_region_id`. Not reproduced in full here; re-derive from Czech administrative regions if needed, this part is pure data entry rather than API knowledge.

### Disposition codes (`category_sub_cb`)

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

(Code `1` is not used by any disposition in the old code — left unmapped.)

## Search response shape

```
{
  "result_size": <int>,           // total matches for the query
  "_embedded": {
    "estates": [
      {
        "_links": { "self": { "href": "/.../<estate_id>" } },
        "gps": { "lat": <float>, "lon": <float> },
        "price_czk": <number> | { "value_raw": <number>, ... },
        ...
      }
    ]
  }
}
```

Two gotchas confirmed by a fix commit (`35db3f3`):
- `price_czk` on search-result estates can be **either** a bare number **or** an object with a `value_raw` field — code that reads it must handle both forms, or it silently drops every rent sample from result sets where the API returns the object form.
- The main search-API axis mix-up above (`category_main_cb`/`category_type_cb` swapped) was itself a real bug that shipped before being caught by this same commit — a reminder that this whole API was learned by trial and error, not from docs.

**[UNVERIFIED]** The estates search endpoint ignores bounding-box (`lat`/`lon`) query params entirely, per the old code's own comment — ids are collected first, then filtered client-side using each estate's `gps` field against a bounding box. Never independently re-confirmed.

## Detail response shape & field extraction

Top-level fields on `GET /api/cs/v2/estates/{id}`: `name` (title string), `locality` (comma-separated location string), `items` (list of `{"name": ..., "value": ...}` facts — the bulk of structured data), `price_czk` / `price` (top-level, see price gotcha below), `text` / `description` (free text), `seo` (block used to reconstruct a canonical URL, see axis-mix-up warning above).

`items` values can be a scalar, a list (first element used), or a nested dict with a `value` key — extraction code must unwrap all three forms.

Field-by-field extraction rules (in old-code priority order), all from the old app's hard-won `fix:` commits:

- **Price**: prefer the `items` entry named **"Celková cena"** (total price) over `"Cena"`, and prefer either of those over the top-level `price_czk`/`price` fields. The top-level field can be a lower base/asking price excluding broker commission, which made a price-cap filter wrongly admit over-budget listings (fix `0a6863f`). If no price item is found at all — e.g. **"Cena na vyžádání"** ("price on request") — skip the listing entirely rather than erroring (fix `9c6b318`).
- **Size (m²)**: `items` in order "Užitná plocha" → "Plocha" → "Celková plocha" → "Podlahová plocha"; as a last resort, regex `(\d+)\s*m²` out of the title string.
- **Year built**: `items` "Rok kolaudace" → "Rok výstavby" → "Rok rekonstrukce"; extract a 4-digit year.
- **Floor**: `items` "Podlaží"; extract the first integer.
- **Energy class**: `items` "Energetická náročnost budovy" → "Energetická třída" → "Energetická náročnost"; a single letter A–G.
- **Construction type**: `items` "Konstrukce budovy" → "Typ budovy" → "Stavba"; normalise via keyword matching (`panel`, `cihla`, `smíšený`, `nízkoenergetický`, `dřevostavba`, `montovaný`, with/without diacritics).
- **Elevator**: three-step fallback — (1) an `items` entry named "Příslušenství"/"vybavení" whose value list contains "výtah"; (2) a standalone `items` entry "Výtah"/"Lift" with value in `ano`/`yes`/`1`/`true`; (3) regex search for "výtah" in the free-text description.
- **Ownership**: `items` "Vlastnictví"/"Vlastnictvi", normalised to `OV` (osobní/personal), `DV` (družstevní/cooperative), or `DV_no_transfer` (cooperative, no transfer rights — this specific sub-case is sometimes only expressed in free text, e.g. "bez možnosti převodu", never as a clean items value).
- **Service charge** ("fond oprav"): `items` "Poplatek za správu domu a pozemku" → "Fond oprav" → "Náklady na bydlení" → "Náklady na správu domu".
- **Disposition**: *not* read from a dedicated `items` field — regex-extracted from the title string, pattern `\d\+(kk|\d)` (e.g. "2+kk", "3+1").
- **City / district from `locality`**: the `locality` string's format varies and must be disambiguated by part count (fix `195735c`, superseding an earlier, wrong, fixed-position assumption):
  - Any part other than the first that starts with "Praha" → `city="Praha"`, `district=<that part>` (e.g. "Praha 6").
  - 3+ parts ("Street, Municipality, Region") → `city = district = parts[-2]` (second-to-last).
  - Exactly 2 parts → ambiguous, resolved by checking the last part for the word "kraj": `"Město, Ústecký kraj"` → first part is the city; `"Ulice, Město"` → last part is the city.
  - 1 part (or empty) → `city = parts[0]` if present, `district = None`.
  - Separately, when mapping a district to a kraj (region) for benchmarking, `"Brno - Žebětín"`-style strings are normalised by taking the part before `" - "`.

## The ~1000-result cap and the bucketing workaround

**[UNVERIFIED]** — inferred from the old app's workaround, not from any API documentation: a single `/api/cs/v2/estates` query appears to cap out at roughly 1000 results (50 pages × 20 per page), even when `result_size` reports more. The old app's `full_market_scan.py` worked around this with a two-level bucketing strategy, used here for a *sale apartments, price-capped* scan:

1. For each of the 14 regions, send a cheap probe (`per_page=1, page=1`) with the target filters (property type, transaction type, `price_to`, `locality_region_id=<region>`) and read `result_size` from the response — no need to paginate for this.
2. If a region's `result_size` is at or under a safety threshold (old app used `BUCKET_LIMIT = 950`, deliberately under 1000), that region is one collection bucket — paginate it directly.
3. If a region's `result_size` exceeds the threshold, subdivide it further: probe every one of the 11 disposition codes within that region (same cheap `per_page=1` probe, now with `category_sub_cb` added), and treat each non-empty `(region, disposition)` pair as its own bucket.
4. Collect IDs from every bucket by paginating with a fixed `per_page` (old app used `20`), reading each estate's numeric ID from `_links.self.href`, until `page * per_page >= result_size` for that bucket.
5. Dedupe all collected IDs across all buckets (a single listing can appear in more than one bucket's result set) with a set.

**Known residual gap, inherited on purpose**: if a single `(region, disposition)` bucket itself still exceeds the threshold, the old app only logged a warning ("some listings may be missed") and did not subdivide further (e.g. by price band). The roadmap's M3 starts price-capped specifically to keep this gap narrow; finer subdivision (e.g. a third level by price band) is deferred unless it proves necessary in practice.

## Rate limiting and retries

- Detail-fetch retries: up to `max_retries` (old default `3`) attempts with exponential backoff (`2^attempt` seconds) on **retriable** failures — HTTP 429 or connection/transient errors.
- **Non-retriable**: HTTP 403 or 404 → skip immediately, no retry.
- A fixed polite delay (old default `0.5s`) was slept after every request attempt regardless of outcome, to cap the steady-state request rate.
- The old app ran region/disposition probes and paginated ID collection on one thread pool, and detail-scrape+score on a separate, smaller thread pool. This concurrency was a Python-app implementation choice, not an API requirement — nothing here implies the new Java app needs the same shapes, only that *some* shared backoff/skip/delay policy is needed (tracked as its own task in M3).

## Rental benchmark lookups

Used to estimate achievable rent for a (city, disposition) pair, via the *same* `/api/cs/v2/estates` search endpoint with `category_main_cb=1` (byty), `category_type_cb=2` (pronájem), `category_sub_cb=<disposition code>`, and `locality_region_id=<city's region>` when the city string maps to one of the 14 regions (otherwise the search runs nationwide). Only page 1 (`per_page=20`) is fetched and treated as a sample, not exhaustive data.

Two gotchas from fix commits:
- `price_czk` on each result estate must be read defensively (bare number or `{"value_raw": ...}` object — see search-response gotcha above); this was a real bug (`35db3f3`) that silently dropped every rent sample until fixed.
- `listing_count` semantics (fix `13a5b96`): when the city *did* map to a `locality_region_id`, the old app used the API's region-wide `result_size` as the count. When it did *not* map to a region, `result_size` would be a misleading nationwide figure — the old app switched to using the actual number of samples collected on page 1 (≤20) instead, specifically to avoid inflating a liquidity/rent-sample metric for cities without a region match.

## Out of scope for v1

- **Bezrealitky** (`bezrealitky.py`) and **iDNES** (`idnes.py`): both HTML-scraped with BeautifulSoup, since neither exposes a public JSON API like Sreality. Noted as existing in the old app; left as a "Later" backlog task, not analysed further here.
- Scoring, ČSÚ demographic/economic data, benchmark tables, and the Streamlit UI from the old app: domain exists but is explicitly out of scope until the "Later" analysis/scoring/UI tasks.

## Biggest open risks

- **[UNVERIFIED]** This is an undocumented internal API, reverse-engineered by a prior AI-generated app through trial and error (see the axis mix-up that shipped as a real bug, fixed in `35db3f3`). It can change without notice. Everything in this document should be spot-checked against a live response before being relied on in the new Java client, not assumed correct from this document alone.
- **[UNVERIFIED]** No mention of Sreality's Terms of Service or `robots.txt` was found anywhere in the old codebase — scraping this internal API's legal status under Sreality's ToS was never checked by the prior app and is not addressed here either.
