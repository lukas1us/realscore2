-- Sets up Metabase's access to Postgres (TASK-25.1). Run by the `metabase-init` compose service on every
-- `docker compose up`, so it must stay idempotent. Passwords are read from env vars set from .env:
--   METABASE_RO_PASSWORD      password of the read-only role `metabase_ro` (Metabase's connection to `realscore`)
--   METABASE_APP_DB_PASSWORD  password of the role `metabase` that owns Metabase's own application DB `metabase`
-- Not a Flyway migration on purpose: no password may end up in a migration.

\set ON_ERROR_STOP on
\getenv ro_password METABASE_RO_PASSWORD
\getenv app_db_password METABASE_APP_DB_PASSWORD

-- Read-only role for exploring the realscore data.
SELECT 'CREATE ROLE metabase_ro LOGIN'
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'metabase_ro') \gexec

ALTER ROLE metabase_ro WITH LOGIN PASSWORD :'ro_password';
ALTER ROLE metabase_ro SET default_transaction_read_only = on;

GRANT CONNECT ON DATABASE realscore TO metabase_ro;
GRANT USAGE ON SCHEMA public TO metabase_ro;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO metabase_ro;
-- Tables created later by Flyway (run as `realscore`) become readable too.
ALTER DEFAULT PRIVILEGES FOR ROLE realscore IN SCHEMA public GRANT SELECT ON TABLES TO metabase_ro;

-- Metabase's own application DB (dashboards, questions, users).
SELECT 'CREATE ROLE metabase LOGIN'
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'metabase') \gexec

ALTER ROLE metabase WITH LOGIN PASSWORD :'app_db_password';

SELECT 'CREATE DATABASE metabase OWNER metabase'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'metabase') \gexec
