---
id: decision-9
title: Metabase for data exploration; custom UI only for proven workflows
date: '2026-11-02T19:59:00Z'
status: accepted
---
## Context

TASK-25 needs a UI to replace the old app's Streamlit frontend. Data is still accumulating, and it is not yet clear which views of it (scrape runs, prices per m², price drops, data quality, a map) are actually worth having. Building a custom UI now would mean guessing its scope.

## Decision

Explore the data with Metabase first, connected directly to the `realscore` Postgres database (TASK-25.1, TASK-25.2). A custom UI, a `web` module built with Thymeleaf + htmx, comes later and covers only the views that proved useful in Metabase (TASK-25.3).

Metabase is read-only: it connects as a dedicated role with SELECT privileges only, created outside Flyway so its password never ends up in a migration. It runs as a separate docker compose service and is not part of the application's module structure (decision-1). The app does not depend on it, and nothing under `src/` changes because of it.

## Consequences

Data can be explored without writing application code, and the custom UI is specified from real usage instead of guesses. Metabase is an extra container with its own state; its dashboards live in its volume, so the SQL of saved questions is kept in the repo under `docs/metabase/*.sql` to survive a lost volume. Metabase queries the tables directly, so schema changes can break saved questions; they are not covered by `mvn verify`. Whatever the custom UI ends up doing goes through the application's modules, not through Metabase.
