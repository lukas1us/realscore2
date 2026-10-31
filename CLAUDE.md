# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`realscore` is a personal project that periodically collects real estate listings from Czech portals (Sreality first) into PostgreSQL, keeps their full history, and later analyses and scores them. It is a rewrite of an older Python app; that app is used only as a source of domain knowledge, captured in the Backlog document "Sreality API".

## Commands

```
mvn test                 # run all tests
mvn test -Dtest=PropertyScoringServiceTest        # run a single test class
mvn test -Dtest=PropertyScoringServiceTest#scoresNewAffordableWellSizedPropertyHighly  # single test method
mvn spring-boot:run      # run the app locally (needs Postgres, see below)
mvn package              # build the jar
```

Tests run against an in-memory H2 database (`src/test/resources/application.properties` overrides the datasource), so `mvn test` does not require a running Postgres instance.

Running the app for real (`spring-boot:run`) needs a reachable Postgres instance, configured via env vars (all have local defaults): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. See `src/main/resources/application.properties`. `docker compose up -d` starts a local Postgres matching those defaults (`docker-compose.yml`).

## Architecture

### Target architecture

- A modular monolith in a single Maven project, packaged by feature under `com.realscore`: `scraping`, `listing`, `parsing`, `analysis`.
- Dependencies go one way only: scraping → listing ← parsing ← analysis. Internals live in an `internal/` subpackage and are never used from another module. A Spring Modulith `ApplicationModules.verify()` test enforces this.
- Store raw first and parse later. Every API response is stored as JSONB in `listing_snapshot`. Normalisation is a separate step that is idempotent and can be re-run. Scraping never computes scores.
- A listing's identity is `UNIQUE(source, external_id)`. Every job execution is recorded in `scrape_run`.
- Schema changes go only through new Flyway migrations. Never edit an existing migration. `ddl-auto` is `validate`.
- DB tests use Testcontainers with Postgres. HTTP tests use JSON fixtures in `src/test/resources/fixtures` and WireMock, never the live API.

### Current state

The current layered scaffold (`model/`, `repository/`, `service/`, `controller/`, `Property`, `PropertyScoringService`, H2, `ddl-auto=update`) is a placeholder. It will be replaced in milestone M1. Do not build on it.

Java version is pinned to 25 in `pom.xml` (`<java.version>`) to match the JDK configured in `.idea/misc.xml`. This requires Spring Boot 4.x — Spring Boot 3.x's bundled ASM version cannot parse Java 25 class files and will fail test discovery with `Unsupported class file major version 69`. Keep these in sync if either changes.

Lombok's annotation processor must be declared explicitly in the `maven-compiler-plugin`'s `annotationProcessorPaths` (see `pom.xml`) — just adding the `lombok` dependency isn't picked up automatically under this compiler plugin/Java version combination, and the failure mode is confusing (`cannot find symbol` on generated getters/setters, not a Lombok-specific error).

## Working rules

- Work only on the Backlog task you were given. No extras or "while I'm at it" changes. Record new ideas as new Backlog tasks instead.
- Before writing code, present a plan (files, classes, steps) and wait for approval.
- Ask before adding any dependency.
- A task is done when `mvn verify` passes, new code has tests, and you've given a short summary of what changed, why, and what I should review.

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