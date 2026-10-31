---
id: decision-7
title: Run everything via docker compose, scraping is a scheduled job
date: '2026-10-30T23:39:00Z'
status: accepted
---
## Context

The old app's scan jobs were run manually from the CLI (`python -m backend.jobs.full_market_scan`); nothing ran on its own schedule. This is a personal project that should collect data continuously with minimal manual intervention, and should be trivial to stand up on a new machine.

## Decision

`docker compose up` brings up the app and Postgres together (the app gets its own service and Dockerfile in M1, alongside the already-existing Postgres service). Inside the running app, a `@Scheduled` job performs the scrape periodically, with the cron expression driven by configuration rather than hardcoded.

## Consequences

Standing up the whole system is one command, and scraping happens on its own without a human remembering to run a script — directly enabling the price-history and delisting tracking that decision-3 and M4 depend on. The cron schedule being config-driven means changing scrape frequency doesn't require a code change or redeploy, just a config/env update.

