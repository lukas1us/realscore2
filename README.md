# realscore

Collects Czech real estate listings from [Sreality](https://www.sreality.cz) into PostgreSQL, keeps their full
history, computes market benchmarks from them (median sale price per m² and median rents per locality and
disposition), and scores flats for sale against them.

## How it works

1. **Scraping**: a scheduled job searches Sreality for flats for sale (under a price cap) and flats for rent. It
   fetches the detail of every new listing, and refetches known ones once their last fetch is older than a
   configurable interval. Every run is recorded in `scrape_run`.
2. **Listing**: every detail response is stored raw as JSONB in `listing_snapshot`, and a new snapshot is written
   only when the payload changes. A listing missing from a run is marked removed, and the mark is cleared if it
   shows up again.
3. **Parsing**: each new or changed snapshot is parsed into a normalised `listing_detail` row: price, size,
   disposition, locality, sale or rent, and so on. Each parse also rebuilds the listing's price history
   (`listing_price_point`) from all its snapshots: one point per asking-price change, skipping snapshots without a
   price. Parsing is idempotent and can be re-run over all stored snapshots.
4. **Analysis**: benchmarks are computed on demand from the parsed details of active listings, a single listing
   can be looked up with its price history and stats, and recent price drops can be listed. Active flats for sale
   are scored 0–100 on demand from their discount against the local sale median, the gross rental yield the local
   rents suggest, and a recent price drop.

The code is a Spring Modulith monolith with one package per module under `com.realscore`: `scraping`, `listing`,
`parsing` and `analysis`. Dependencies go one way only (scraping → listing ← parsing ← analysis), and `ModularityTests`
enforces this. The schema is managed only by Flyway migrations (`src/main/resources/db/migration`).

## Requirements

- Java 25
- Maven
- Docker, for the local PostgreSQL 16 and for the tests (Testcontainers)

## Running locally

Build and start the app together with PostgreSQL:

```
docker compose up -d --build
```

The app is available on `http://localhost:8080`. Compose runs the scrape job daily at 03:00 Europe/Prague
(`SCRAPE_CRON`) and health-checks the app via `/actuator/health`.

To run the app on the host instead, start only PostgreSQL and then the app:

```
docker compose up -d postgres
mvn spring-boot:run
```

On the host, scraping is off unless `SCRAPE_CRON` is set.

## Configuration

Set through environment variables; defaults are in `src/main/resources/application.properties`.

| Variable | Default | Meaning |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `localhost`, `5432`, `realscore`, `realscore`, `realscore` | PostgreSQL connection |
| `SCRAPE_CRON` | `-` (disabled) | Spring cron expression (6 fields, Europe/Prague time) for the scrape job |
| `SCRAPE_PRICE_CAP` | `5000000` | Maximum price in CZK of flats for sale |
| `SCRAPE_RENT_PRICE_CAP` | empty (no cap) | Maximum monthly rent in CZK of flats for rent |
| `SCRAPE_REFETCH_AFTER` | `7d` | A known listing is fetched again once its last fetch is older than this; `0` refetches on every run |

Analysis settings (Spring properties, e.g. as `REALSCORE_ANALYSIS_MIN_SAMPLE_SIZE`):

| Property | Default | Meaning |
|---|---|---|
| `realscore.analysis.large-cities` | `Praha,Brno,Ostrava` | Cities grouped by city part instead of as a whole |
| `realscore.analysis.min-sample-size` | `5` | Fewest listings a group needs; smaller groups fall back to city, then district, then region |
| `realscore.analysis.price-drop-days` | `30` | Default window in days of `GET /api/analysis/price-drops` |
| `realscore.analysis.scores-limit` | `50` | Default number of results of `GET /api/analysis/scores` |
| `realscore.analysis.scoring.discount.{min,max,weight}` | `-30`, `30`, `50` | Listing score part: % cheaper per m² than the local sale median |
| `realscore.analysis.scoring.rental-yield.{min,max,weight}` | `2`, `8`, `35` | Listing score part: gross yearly rent (from the local rent medians) over the price, in % |
| `realscore.analysis.scoring.price-drop.{min,max,weight}` | `0`, `15`, `15` | Listing score part: % price drop over the price-drop window (0 without a drop) |

Each listing score part maps linearly to 0–100 between `min` and `max` (clamped), and the total is the weighted
average of the parts that have data. A flat with neither a sale nor a rent benchmark is not scored. Only active flats
for sale with a price, size, disposition and region are scored.

Sreality client settings (`realscore.sreality.*`: page size, retries, request pacing) are in `application.properties`
too.

## HTTP API

| Endpoint | Description |
|---|---|
| `GET /api/analysis/benchmarks/sale-price-per-m2` | Median asking price per m² of active flats for sale, per locality and disposition, with the locality level used and the sample size |
| `GET /api/analysis/benchmarks/rent` | Median monthly rent and median rent per m² of active flats for rent, per locality and disposition, each with its level and sample size |
| `GET /api/listings/{source}/{externalId}` | One listing, removed or not: first/last seen and removed time, parsed details (`null` if never parsed), price history oldest first, and stats: days on market, number of price drops, % change from the first to the current price. Also its `score` (as in `/api/analysis/scores`) if it is an active flat for sale that can be scored, else `null`. 404 if unknown |
| `GET /api/analysis/scores[?limit=50][&region=…][&city=…][&disposition=…]` | Scored active flats for sale, best first, at most `limit` (default `realscore.analysis.scores-limit`). Each with its total, and per part (discount, rental yield, price drop) the raw %, sub-score, weight share and the benchmark used. Filters match exactly, ignoring case; `disposition` is the label such as `2+kk`, with `+` encoded as `%2B` in the URL. `limit` < 1 gives 400 |
| `GET /api/analysis/price-drops[?days=30][&type=sale\|rent]` | Active listings whose current price is below the price they had `days` ago (default `realscore.analysis.price-drop-days`), or below their first price if they appeared later; several drops add up. Each with old and new price, percent drop and when the current price was first seen; largest drop first. Without `type`, sales and rentals both; `days` < 1 or another `type` gives 400 |
| `POST /api/parsing/reparse?source=sreality[&externalId=…]` | Re-parses stored snapshots: all listings of the source, or only the given external IDs (repeatable), and rebuilds their price history. Synchronous; returns the `listing_detail` counts |
| `GET /actuator/health` | Health check |

## Operations notes

- **After a parser or schema change** (e.g. a new `listing_detail` column), run
  `POST /api/parsing/reparse?source=sreality` once so existing rows pick it up. Until then, new columns stay empty,
  and listings missing a field the benchmarks need are left out of them. The same applies after upgrading to the
  price history: listings parsed before it have no history until re-parsed.
- **The first run after a new search is added** (e.g. rentals) fetches every matching listing's detail at about two
  requests per second, which can take hours. Later runs only fetch new and due listings.
- **Days on market count from the first time realscore saw a listing**, not from when it was published: Sreality's
  detail has no publication date. Listings already online before the first scrape start at 0.
- **Sale benchmarks are capped**: sales are only collected below `SCRAPE_PRICE_CAP`, so in expensive areas the medians
  describe the cheaper end of the market. Check the sample sizes.
- The Sreality API is undocumented and has changed shape before. What is known about it is in the Backlog document
  "Sreality API".

## Testing

```
mvn verify
```

Tests run against PostgreSQL in Testcontainers (Docker must be running) and against WireMock with JSON fixtures in
`src/test/resources/fixtures`. They never call the live Sreality API. CI runs the same command on every push.
