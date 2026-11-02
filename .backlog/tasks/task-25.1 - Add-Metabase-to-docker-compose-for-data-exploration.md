---
id: TASK-25.1
title: Add Metabase to docker compose for data exploration
status: To Do
assignee: []
created_date: '2026-11-02T19:59:00Z'
labels:
  - infra
  - ui
dependencies: []
parent_task_id: TASK-25
ordinal: 53000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
We want to explore the collected data (scrape runs, listings, parsed details, prices) while it accumulates, without building a UI yet. Metabase connected directly to Postgres gives that cheaply. It is an infra-only change: nothing under `src/` changes and Metabase is not part of the app's module structure.

Scope:
- A new `metabase` service in `docker-compose.yml`, with the image pinned to a specific version (not `latest`), depending on the `postgres` service being healthy.
- Metabase's own application DB is stored in a named volume. Choose between its default embedded H2 file and a separate `metabase` database in our Postgres, and note the trade-off in the implementation plan.
- Metabase connects to the `realscore` DB as a dedicated READ-ONLY role: SELECT only, including on tables created later (default privileges). The role must NOT be created by a Flyway migration, because its password must not end up in a migration. The implementation plan must pick and justify a mechanism: a compose init script (note it only runs on a fresh Postgres volume, so existing volumes need another path), a one-off documented SQL step, or something else.
- Credentials only via env vars / `.env` (with placeholders in `.env.example`), never committed.
- Port bound to localhost only: `127.0.0.1:3000`.
- A README section: how to start it, the first-time Metabase setup, and how to add the DB connection with the read-only role.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `docker compose up` starts app, postgres and metabase, and all three become healthy/reachable
- [ ] #2 As the read-only role, SELECT works and INSERT/UPDATE/DELETE/DDL fail; this is verified and the check is documented
- [ ] #3 The app and `mvn verify` are unaffected: no changes under `src/`
- [ ] #4 README documents starting Metabase, first-time setup, adding the read-only DB connection, and how credentials are handled
<!-- AC:END -->
