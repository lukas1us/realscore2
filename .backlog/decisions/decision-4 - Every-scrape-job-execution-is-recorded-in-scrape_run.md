---
id: decision-4
title: Every scrape job execution is recorded in scrape_run
date: '2026-10-30T23:39:00Z'
status: accepted
---
## Context

The old app's scan jobs (`full_market_scan.py`, `rent_market_scan.py`) only logged to stdout; there was no durable record of when a scan ran, how many listings it found/saved, or what errors occurred, which makes diagnosing "why didn't listing X update" or "did last night's job even run" a matter of digging through logs if they still exist.

## Decision

Every execution of a scraping job — manual or scheduled — is recorded as a row in `scrape_run`, capturing status, counts (found/new/updated/removed/errored), and error details.

## Consequences

Operational questions ("is the scraper healthy", "when did this listing last get checked") become a query against `scrape_run` instead of log archaeology. M4's new/changed/removed detection and scheduled job both depend on this table existing first, which is why `scrape_run` is its own task ahead of the scheduler task in that milestone.

