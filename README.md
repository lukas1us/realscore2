# realscore

Collects Czech real estate listings from [Sreality](https://www.sreality.cz) into PostgreSQL, keeps their full
history, computes market benchmarks from them (median sale price per m² and median rents per locality and
disposition), and scores flats for sale against them.

## How it works

1. **Scraping**: a scheduled job searches Sreality for flats for sale (under a price cap) and flats for rent. It
   fetches the detail of every new listing, and refetches known ones once their last fetch is older than a
   configurable interval, or in the same run when their price in the search results changed since the last run.
   Every run is recorded in `scrape_run`.
2. **Listing**: every detail response is stored raw as JSONB in `listing_snapshot`, minus the seller's contact data
   (the broker's name, email and phones, and the agency's web URLs are removed first). A new snapshot is written only
   when the payload changes; whitespace and key order do not count. Dropping out of the search results alone is no proof a listing is gone (its price
   may have risen above the cap), so after each successful run the detail of every active listing the run did not
   see is fetched: 404 or 410 marks it removed, 200 records its snapshot and keeps it active, and 403 or any other
   failure leaves it active to be checked again next run. The removed mark is cleared if the listing shows up again.
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

Compose needs a `.env` file with the Metabase passwords (see [Metabase](#metabase)); without it every
`docker compose` command fails. Create it once:

```
cp .env.example .env    # then replace the placeholder passwords
```

Build and start the app together with PostgreSQL and Metabase:

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

## Metabase

[Metabase](https://www.metabase.com/) is for exploring the collected data until a custom UI exists. It reads the
`realscore` database directly, as a read-only role, and is not part of the app.

`docker compose up -d` starts it together with the app on `http://localhost:3000` (bound to localhost only). The first
start takes a minute or two; it is ready when `docker compose ps` shows `metabase` as healthy.

**Credentials.** Two passwords, both set only in `.env` (git-ignored; `.env.example` has placeholders):

| Variable | Used for |
|---|---|
| `METABASE_RO_PASSWORD` | Role `metabase_ro`, Metabase's read-only connection to the `realscore` DB |
| `METABASE_APP_DB_PASSWORD` | Role `metabase`, owner of Metabase's own application DB `metabase` (dashboards, questions, users) |

Both roles and the `metabase` database are created by the one-shot `metabase-init` service, which runs
`docker/metabase/init.sql` on every `docker compose up`. The script is idempotent and works on a fresh or an existing
Postgres volume. It is not a Flyway migration, so no password ends up in a migration. Changing a password in `.env`
takes effect at the next `docker compose up`. After changing `METABASE_RO_PASSWORD`, also update it in Metabase's
connection settings.

**First-time setup.** Open `http://localhost:3000`, then:

1. Create the admin account. It is stored in Metabase's own DB, not in `.env`.
2. When asked to add data (or later under *Admin settings → Databases → Add database*), choose PostgreSQL with:
   - Host `postgres`, port `5432`, database name `realscore`
   - Username `metabase_ro`, password the value of `METABASE_RO_PASSWORD` from `.env`

Metabase's state lives in the `metabase` database inside the `postgres-data` volume, so `docker compose down -v`
deletes it too. The SQL of the exploration dashboards is meant to be kept in `docs/metabase/` (TASK-25.2), so they can be rebuilt.

**Read-only access.** `metabase_ro` can only SELECT, also on tables created by later Flyway migrations, and its
sessions are read-only by default. To check it:

```
docker compose exec -e PGPASSWORD="$(grep ^METABASE_RO_PASSWORD= .env | cut -d= -f2-)" postgres \
  psql -h localhost -U metabase_ro -d realscore \
  -c 'SELECT count(*) FROM listing' \
  -c 'INSERT INTO scrape_run DEFAULT VALUES' \
  -c 'DELETE FROM listing WHERE false' \
  -c 'CREATE TABLE probe (id int)'
```

The SELECT returns a count. Every write fails with `cannot execute … in a read-only transaction`. With the read-only
session default switched off (`PGOPTIONS='-c default_transaction_read_only=off'`), writes still fail, with
`permission denied` or `must be owner`. Like every Postgres role, `metabase_ro` can create temporary tables. They are
private to its session and disappear when it ends.

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
| `realscore.analysis.scoring.discount.{min,max,weight}` | `-30`, `30`, `45` | Listing score part: % cheaper per m² than the local sale median |
| `realscore.analysis.scoring.rental-yield.{min,max,weight}` | `2`, `8`, `30` | Listing score part: gross yearly rent (from the local rent medians) over the price, in % |
| `realscore.analysis.scoring.price-drop.{min,max,weight}` | `0`, `15`, `10` | Listing score part: % price drop over the price-drop window (0 without a drop) |
| `realscore.analysis.scoring.quality.weight` | `15` | Listing score part: quality of the flat itself (points below) |
| `realscore.analysis.scoring.quality.ownership.{personal,cooperative}` | `10`, `-10` | Quality points for OV and DV ownership |
| `realscore.analysis.scoring.quality.construction.{brick,panel}` | `10`, `-5` | Quality points for Cihlová and Panelová construction |
| `realscore.analysis.scoring.quality.energy-class.{a,b,c,d,e,f,g}` | `10`, `7`, `3`, `0`, `-3`, `-7`, `0` | Quality points per energy class (G is what sellers without a certificate state) |
| `realscore.analysis.scoring.quality.building-condition.{new-build,after-reconstruction,very-good,good,before-reconstruction,poor,under-construction,project,in-reconstruction}` | `10`, `10`, `5`, `0`, `-10`, `-20`, `0`, `0`, `0` | Quality points for Novostavba, Po rekonstrukci, Velmi dobrý, Dobrý, Před rekonstrukcí, Špatný, Ve výstavbě, Projekt, V rekonstrukci |
| `realscore.analysis.scoring.quality.elevator.{from-floor,missing}` | `4`, `-10` | Quality points for no elevator (an explicit "Ne") from this floor up; 0 is the ground floor |

Each listing score part maps linearly to 0–100 between `min` and `max` (clamped), and the total is the weighted
average of the parts that have data. The quality part instead starts at 50 and adds the points of each factor,
clamped to 0–100; a factor the listing does not state, or a value not listed above (e.g. Smíšená construction or
K demolici), adds 0, so a missing factor is never penalised. A flat with neither a sale nor a rent benchmark is not
scored. Only active flats for sale with a price, size, disposition and region are scored.

Sreality client settings (`realscore.sreality.*`: page size, retries, request pacing) are in `application.properties`
too.

## HTTP API

| Endpoint | Description |
|---|---|
| `GET /api/analysis/benchmarks/sale-price-per-m2` | Median asking price per m² of active flats for sale, per locality and disposition, with the locality level used and the sample size |
| `GET /api/analysis/benchmarks/rent` | Median monthly rent and median rent per m² of active flats for rent, per locality and disposition, each with its level and sample size |
| `GET /api/listings/{source}/{externalId}` | One listing, removed or not: first/last seen and removed time, parsed details (`null` if never parsed), price history oldest first, and stats: days on market, number of price drops, % change from the first to the current price. Also its `score` (as in `/api/analysis/scores`) if it is an active flat for sale that can be scored, else `null`. 404 if unknown |
| `GET /api/analysis/scores[?limit=50][&region=…][&city=…][&disposition=…]` | Scored active flats for sale, best first, at most `limit` (default `realscore.analysis.scores-limit`). Each with its total, and per part (discount, rental yield, price drop) the raw %, sub-score, weight share and the benchmark used. The `quality` part has its sub-score, weight share, summed `points` and every factor (`ownership`, `construction`, `energyClass`, `buildingCondition`, `elevator`) with its parsed `value` (`null` if unknown) and `points`. Filters match exactly, ignoring case; `disposition` is the label such as `2+kk`, with `+` encoded as `%2B` in the URL. `limit` < 1 gives 400 |
| `GET /api/analysis/price-drops[?days=30][&type=sale\|rent]` | Active listings whose current price is below the price they had `days` ago (default `realscore.analysis.price-drop-days`), or below their first price if they appeared later; several drops add up. Each with old and new price, percent drop and when the current price was first seen; largest drop first. Without `type`, sales and rentals both; `days` < 1 or another `type` gives 400 |
| `POST /api/parsing/reparse?source=sreality[&externalId=…]` | Re-parses stored snapshots: all listings of the source, or only the given external IDs (repeatable), and rebuilds their price history. Synchronous; returns the `listing_detail` counts |
| `GET /actuator/health` | Health check |

## Operations notes

- **After a parser or schema change** (e.g. a new `listing_detail` column), run
  `POST /api/parsing/reparse?source=sreality` once so existing rows pick it up. Until then, new columns stay empty,
  and listings missing a field the benchmarks need are left out of them. The same applies after upgrading to:
  - the price history: listings parsed before it have no history until re-parsed;
  - the not-entered elevator and energy class fix: until re-parsed, a not-entered elevator stays `false` instead of
    `NULL` and a not-chosen energy class stays `-` instead of `NULL`;
  - building condition: `building_condition_code` and `building_condition_label` (in the listing endpoint
    `buildingConditionCode` and `buildingConditionLabel`, Sreality's raw code and label, `null` when not entered)
    stay empty until re-parsed, so the score's building condition factor adds 0 until then;
  - the split building years: `year_built` is replaced by `acceptance_year` and `reconstruction_year` (in the
    listing endpoint `acceptanceYear` and `reconstructionYear`), and both stay empty until re-parsed.
- **The first run after a new search is added** (e.g. rentals) fetches every matching listing's detail at about two
  requests per second, which can take hours. Later runs only fetch new and due listings, plus every active listing
  the run did not see: one that stays online outside the searches (e.g. above the price cap) is fetched on every run
  until it is gone. In `scrape_run`, changed snapshots from that check count as `updated_count`, and 403s and other
  failures as `error_count`.
- **A price change is seen in the run that finds it.** Each run keeps every listing's price from the search results
  (`listing.search_price_czk`, Sreality's raw value, `0` for price on request) and refetches the detail of any listing
  whose search price differs from the last run's, without waiting for `SCRAPE_REFETCH_AFTER`. The new price point
  is dated to that run. If that fetch fails, the old search price is kept, so the next run tries again. Such
  refetches count in `scrape_run` like any other (`updated_count` when the snapshot changed). The first run after
  upgrading only stores the search prices, since there is nothing to compare them with yet.
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
