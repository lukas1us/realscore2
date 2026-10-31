---
id: decision-6
title: DB tests use Testcontainers Postgres, HTTP tests use fixtures and WireMock
date: '2026-10-30T23:39:00Z'
status: accepted
---
## Context

The current test setup runs against H2 (see `src/test/resources/application.properties`), which doesn't support JSONB or the Postgres-specific behaviour the target schema depends on (e.g. `listing_snapshot`'s payload column). Tests also must never depend on Sreality's live API — it's an undocumented internal API that can change or rate-limit without notice, and the old app never had automated tests covering it at all.

## Decision

Tests that touch the database use Testcontainers with a real Postgres image, replacing H2 entirely. Tests that touch HTTP (the Sreality client) use JSON fixtures recorded under `src/test/resources/fixtures` served through WireMock, never the live API.

## Consequences

Tests get slower to start (container boot time) but exercise real Postgres semantics (JSONB, constraints) that H2 can only approximate, and stay deterministic and offline-runnable regardless of Sreality's availability or API changes. Every new fixture needs to be captured once from a real (or representative) response and committed, which is the ongoing cost of this approach — but it also means fixtures double as regression tests for parsing gotchas like the ones catalogued in the "Sreality API" doc.

