# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`realscore` is a personal project that periodically collects real estate listings from Czech portals (Sreality first) into PostgreSQL, keeps their full history, and later analyses and scores them. It is a rewrite of an older Python app; that app is used only as a source of domain knowledge, captured in the Backlog document "Sreality API".

## Commands

```
mvn verify               # run all tests and checks (needs Docker for Testcontainers)
mvn test -Dtest=MarketBenchmarkServiceTest        # run a single test class
mvn test -Dtest=MarketBenchmarkServiceTest#saleMedianIsOverActiveSaleFlatsWithPriceAndSize  # single test method
docker compose up -d --build   # build and run the app + Postgres (app on :8080)
mvn spring-boot:run      # run the app on the host (needs Postgres, see below)
mvn package              # build the jar
```

DB tests run against PostgreSQL 16 in Testcontainers (`PostgresContainerConfig`, imported by `@SpringBootTest` tests), so Docker must be running; no local Postgres is needed. HTTP tests use WireMock with JSON fixtures in `src/test/resources/fixtures`.

Running the app for real (`spring-boot:run`) needs a reachable Postgres instance, configured via env vars (all have local defaults): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. See `src/main/resources/application.properties`. `docker compose up -d postgres` starts only the local Postgres matching those defaults (`docker-compose.yml`); `docker compose up -d` also starts the app container (`Dockerfile`), which points at that Postgres.

## Architecture

### Target architecture

- A modular monolith in a single Maven project, packaged by feature under `com.realscore`: `scraping`, `listing`, `parsing`, `analysis`.
- Dependencies go one way only: scraping → listing ← parsing ← analysis. Internals live in an `internal/` subpackage and are never used from another module. A Spring Modulith `ApplicationModules.verify()` test enforces this.
- Store raw first and parse later. Every API response is stored as JSONB in `listing_snapshot`. Normalisation is a separate step that is idempotent and can be re-run. Scraping never computes scores.
- A listing's identity is `UNIQUE(source, external_id)`. Every job execution is recorded in `scrape_run`.
- Schema changes go only through new Flyway migrations. Never edit an existing migration. `ddl-auto` is `validate`.
- DB tests use Testcontainers with Postgres. HTTP tests use JSON fixtures in `src/test/resources/fixtures` and WireMock, never the live API.

### Current state

The target architecture above is in place. `scraping` runs the scheduled Sreality collection of flats for sale and for rent (`SrealityScrapeJob`, recorded in `scrape_run`); `listing` stores listings and raw snapshots and detects removed ones; `parsing` turns each new or changed snapshot into a `listing_detail` row (automatically via an event from `listing`, or on demand via `POST /api/parsing/reparse`) and rebuilds the per-listing price history (`listing_price_point`), and exposes `ListingDetailsQueryService` for reading; `analysis` computes everything on demand from parsed data: sale and rent benchmarks (`MarketBenchmarkService`), a single listing with its price history and stats (`ListingOverviewService`), recent price drops (`PriceDropService`), and a 0-100 score per active sale flat (`ListingScoreService`). Quality factors in the score (TASK-35) do not exist yet. `README.md` documents endpoints and configuration.

Java version is pinned to 25 in `pom.xml` (`<java.version>`) to match the JDK configured in `.idea/misc.xml`. This requires Spring Boot 4.x — Spring Boot 3.x's bundled ASM version cannot parse Java 25 class files and will fail test discovery with `Unsupported class file major version 69`. Keep these in sync if either changes.

Lombok's annotation processor must be declared explicitly in the `maven-compiler-plugin`'s `annotationProcessorPaths` (see `pom.xml`) — just adding the `lombok` dependency isn't picked up automatically under this compiler plugin/Java version combination, and the failure mode is confusing (`cannot find symbol` on generated getters/setters, not a Lombok-specific error).

## Working rules

- Work only on the Backlog task you were given. No extras or "while I'm at it" changes. Record new ideas as new Backlog tasks instead.
- Before writing code, present a plan (files, classes, steps) and wait for approval.
- Ask before adding any dependency.
- Always use Lombok for boilerplate (getters/setters, constructors, builders) instead of hand-writing it.
- A task is done when `mvn verify` passes, new code has tests, `README.md` reflects any change to endpoints, configuration, run commands or documented behaviour, and you've given a short summary of what changed, why, and what I should review.

<!-- BACKLOG.MD GUIDELINES START -->
<!-- backlog.md-instructions-version: 1.55.3 -->
<CRITICAL_INSTRUCTION>

## Backlog.md Workflow

This project uses Backlog.md for task and project management.

**At the beginning of each conversation in this project, run `backlog instructions overview` before answering or taking action. Re-read it only if you have not read it yet in the current conversation.**

Use the overview to decide whether to search, read, create, or update Backlog tasks.

Before task lifecycle actions, read the matching detailed guide:
- `backlog instructions task-creation` before creating or splitting tasks
- `backlog instructions task-execution` before planning, changing status or assignee, adding a plan or implementation notes, or implementing task work
- `backlog instructions task-finalization` before checking acceptance criteria, writing final summaries, or moving tasks to terminal statuses

Use `backlog <command> --help` before running unfamiliar commands. Help shows options, fields, and examples.

Do not edit Backlog task, draft, document, decision, or milestone markdown files directly. Use the `backlog` CLI so metadata, relationships, and history stay consistent.

</CRITICAL_INSTRUCTION>
<!-- BACKLOG.MD GUIDELINES END -->

## Git

- Work directly on `main`. Leave changes uncommitted until I approve them. Never push, rebase, amend, or force anything. I do that.
- After `/finish-task` passes and I approve: make ONE commit containing the code AND the Backlog task files changed by the CLI.
- Exception: if a task is experimental or will need several commits, ask me first whether to create a branch `task-<id>-<short-desc>`.
- Commit message format (Conventional Commits):

  <type>(<module>): <imperative summary, max 72 chars> [TASK-<id>]

  <optional body: why, not what — 1–3 lines>

  type: feat | fix | refactor | test | chore | docs
  module: scraping | listing | parsing | analysis | infra | backlog

  Example: `feat(scraping): add Sreality detail client with retry on 429 [TASK-12]`
- Never stage `.env`, IDE files, build output nor anything else stated in `.gitignore`. Check `git status` before committing.

## Dependency management

- Never add a dependency directly. Use the add-dependency skill and wait for my approval.