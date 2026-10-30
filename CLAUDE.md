# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`realscore` is a Spring Boot (4.0.9) service for scoring real estate properties, backed by PostgreSQL. It's an early-stage scaffold — currently one entity (`Property`), one scoring heuristic, and basic CRUD endpoints.

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

Standard layered Spring Boot structure under `com.realscore`:

- `model/` — JPA entities (`Property`). Uses Lombok (`@Getter`/`@Setter`/`@NoArgsConstructor`) to cut boilerplate — the protected no-arg constructor is Lombok-generated (required by Hibernate), the explicit field constructor is hand-written since Lombok's `@AllArgsConstructor` would wrongly include the generated `id`.
- `repository/` — Spring Data `JpaRepository` interfaces.
- `service/` — business logic. `PropertyScoringService` computes a 0–100 score from price-per-square-foot, building age, and bed/bath count, weighted 50/30/20. It takes a `Clock` in its constructor (defaulting to the system clock) specifically so age-based scoring is deterministic in tests — pass a fixed `Clock` rather than hardcoding "now" when testing anything age-related.
- `controller/` — REST endpoints under `/api/properties` (list, create, get by id, get score by id).

`spring.jpa.hibernate.ddl-auto=update` is used for local/dev convenience (schema auto-created/updated from entities) — there are no migration tools (Flyway/Liquibase) set up yet.

Java version is pinned to 25 in `pom.xml` (`<java.version>`) to match the JDK configured in `.idea/misc.xml`. This requires Spring Boot 4.x — Spring Boot 3.x's bundled ASM version cannot parse Java 25 class files and will fail test discovery with `Unsupported class file major version 69`. Keep these in sync if either changes.

Lombok's annotation processor must be declared explicitly in the `maven-compiler-plugin`'s `annotationProcessorPaths` (see `pom.xml`) — just adding the `lombok` dependency isn't picked up automatically under this compiler plugin/Java version combination, and the failure mode is confusing (`cannot find symbol` on generated getters/setters, not a Lombok-specific error).

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
