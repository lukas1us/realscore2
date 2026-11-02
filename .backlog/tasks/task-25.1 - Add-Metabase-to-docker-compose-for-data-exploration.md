---
id: TASK-25.1
title: Add Metabase to docker compose for data exploration
status: Done
assignee:
  - '@claude'
created_date: '2026-11-02T19:59:00Z'
updated_date: '2026-11-02T20:39:00Z'
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
- [x] #1 `docker compose up` starts app, postgres and metabase, and all three become healthy/reachable
- [x] #2 As the read-only role, SELECT works and INSERT/UPDATE/DELETE/DDL fail; this is verified and the check is documented
- [x] #3 The app and `mvn verify` are unaffected: no changes under `src/`
- [x] #4 README documents starting Metabase, first-time setup, adding the read-only DB connection, and how credentials are handled
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. `docker-compose.yml`: add a one-shot `metabase-init` service (image `postgres:16`, same as the DB) that runs `docker/metabase/init.sql` with `psql` against `postgres` on every `docker compose up`, after `postgres` is healthy. Add a `metabase` service on image `metabase/metabase:v0.65.2.5` that depends on `metabase-init` having completed successfully, binds `127.0.0.1:3000:3000`, and has a healthcheck on `/api/health`.
2. `docker/metabase/init.sql` (idempotent, passwords passed as psql variables from env, never in the file):
   - read-only role `metabase_ro`: create if missing, set password from env, `default_transaction_read_only = on`, CONNECT on `realscore`, USAGE on schema `public`, SELECT on all existing tables, and `ALTER DEFAULT PRIVILEGES FOR ROLE realscore IN SCHEMA public GRANT SELECT ON TABLES` so tables from future Flyway migrations are covered.
   - Metabase app DB: role `metabase` plus database `metabase` owned by it, both created if missing.
3. Mechanism for the read-only role: a compose init service that runs on every `up`. A `/docker-entrypoint-initdb.d` script would only run on a fresh volume, and the existing `postgres-data` volume already exists, so that path would also need a manual step. A one-off documented SQL step would be easy to forget and would drift from `.env`. Running an idempotent script on every `up` handles fresh and existing volumes the same way and keeps the passwords in sync with `.env`. Not a Flyway migration, so no password ends up in a migration, and `src/` is untouched.
4. Metabase app DB: a separate `metabase` database in our Postgres, not embedded H2. Trade-off: H2 needs no extra role or database, but it is a single file that can get corrupted on an unclean shutdown, and Metabase advises against it beyond trying it out. Postgres is more robust and its data lives in the existing named volume `postgres-data`, so a single volume holds both DBs. The cost is one more role/database, created by the same init script, and Metabase state shares the volume with the app data.
5. Credentials: `METABASE_RO_PASSWORD` and `METABASE_APP_DB_PASSWORD` come from `.env` via `${VAR:?}`, so compose fails fast if they are missing. Add `.env.example` with placeholders. Add `.env` to `.gitignore` and `.dockerignore`. The existing `realscore` DB credentials stay as they are (out of scope).
6. README: a Metabase section covering `cp .env.example .env`, startup, first-time setup at http://localhost:3000, adding the `realscore` connection as `metabase_ro`, credentials handling, and a psql check that SELECT works while INSERT/UPDATE/DELETE/DDL fail. Also note in "Running locally" that `.env` is now needed for any `docker compose` command.
7. Verify: `docker compose up -d --build` brings up app, postgres and metabase as healthy, with metabase-init exiting 0. Run the read-only checks as `metabase_ro` and record the output in the notes. Run `mvn verify`. Confirm `git status` shows no changes under `src/`.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Verified 2026-11-02 against the existing local postgres-data volume (19,597 listings):
- `docker compose up -d --build`: metabase-init exited 0; postgres, app and metabase all healthy; `/api/health` gave {"status":"ok"} on 127.0.0.1:3000. Metabase created 182 tables in the `metabase` DB and none in `realscore`.
- As metabase_ro: SELECT works. INSERT/UPDATE/DELETE/CREATE TABLE fail with "cannot execute ... in a read-only transaction". With default_transaction_read_only switched off they still fail: "permission denied for table/schema", and "must be owner" for DROP TABLE.
- A table created afterwards by `realscore` was readable by metabase_ro straight away (default privileges).
- Re-running metabase-init (`docker compose run --rm metabase-init`) succeeds, so the script is idempotent.
- The README check command was run verbatim with the expected output.
- `mvn verify`: 239 tests, 0 failures/errors. Nothing under src/ changed by this task.
- Image pinned to metabase/metabase:v0.65.2.5, the newest specific version tag on Docker Hub on 2026-11-02.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added Metabase to docker compose for exploring the data while it accumulates, with no change to the app.
- New `metabase-init` one-shot service runs the idempotent `docker/metabase/init.sql` on every `up`. The script creates or updates the read-only role `metabase_ro` (SELECT on current and future tables, read-only sessions by default) and the role and database `metabase` for Metabase's own data. Because it runs on every `up`, it works on fresh and existing volumes, and no password ends up in a migration.
- New `metabase` service on the pinned image v0.65.2.5, bound to 127.0.0.1:3000, with a health check. Its app DB is in Postgres rather than H2, which is more robust, and it lives in the `postgres-data` volume.
- Passwords come only from `.env` (`.env.example` has placeholders; `.env` is git- and docker-ignored). Compose fails fast without them.
- README has a new Metabase section covering setup, connection, credentials and the read-only check.
Risk: `.env` is now required for every `docker compose` command, including `up -d postgres`. Temporary tables stay possible for metabase_ro, as for any role.
<!-- SECTION:FINAL_SUMMARY:END -->
