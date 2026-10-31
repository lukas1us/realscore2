---
id: decision-1
title: Modular monolith with one-way module dependencies
date: '2026-10-30T23:39:00Z'
status: accepted
---
## Context

The previous Python app grew into a single undifferentiated codebase where scraping, persistence, and scoring logic were tangled together, making it hard to change one concern without risking another. The rewrite should prevent that from recurring without going as far as separate services/deployables, which this project's scale does not need.

## Decision

A single Maven project, packaged by feature under `com.realscore`: `scraping`, `listing`, `parsing`, `analysis`. Dependencies go one way only: scraping → listing ← parsing ← analysis. Each module's internals live in an `internal` subpackage and are never referenced from another module — only a module's non-`internal` API is. Boundaries are enforced automatically by a Spring Modulith `ApplicationModules.verify()` test, not just by convention.

## Consequences

Refactors within a module are cheap; changes that would cross the dependency arrow the wrong way (e.g. `listing` reaching into `parsing`) fail CI immediately via the Modulith test instead of being caught in review or not at all. The cost is some upfront ceremony (package-info per module, the verify test, discipline about what's public vs. `internal`) before any feature code exists — paid once in M1.

