---
id: decision-5
title: Schema changes only via new Flyway migrations, ddl-auto=validate
date: '2026-10-30T23:39:00Z'
status: accepted
---
## Context

The current scaffold uses Hibernate's `ddl-auto=update`, which silently alters the schema to match entity definitions — convenient for a throwaway placeholder, but it hides schema drift and makes it impossible to know what changed between deploys or to roll back a bad change deliberately.

## Decision

All schema changes go through new Flyway migration files; existing migrations are never edited once committed. `ddl-auto` is switched to `validate`, so Hibernate only checks the schema matches the entities and never mutates it.

## Consequences

Every schema change is explicit, reviewable, and ordered — exactly what M1's "add Flyway" task establishes before any feature module needs a table. The cost is that every entity change now requires writing a migration by hand instead of letting Hibernate infer one, which is an explicit tradeoff: more upfront friction per change, in exchange for a schema history that can be trusted and replayed (including by Testcontainers-based tests).

