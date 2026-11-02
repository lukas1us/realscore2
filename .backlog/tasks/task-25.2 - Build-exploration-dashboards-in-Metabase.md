---
id: TASK-25.2
title: Build exploration dashboards in Metabase
status: To Do
assignee: []
created_date: '2026-11-02T19:59:00Z'
labels:
  - ui
dependencies:
  - TASK-25.1
parent_task_id: TASK-25
ordinal: 54000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Manual work done by the user in the Metabase UI, on top of TASK-25.1. The point is to find out which views of the data are actually useful before building a custom UI (TASK-25.3). This task captures which questions the dashboards must answer:

- **Scrape runs**: latest `scrape_run` rows, their duration and counts, and failed or ABORTED runs.
- **Prices**: median CZK/m² by city/citypart and disposition, with the sample size shown.
- **Changes**: new listings and price drops since the last run.
- **Data quality**: null rates per `listing_detail` column, energy class distribution, suspected duplicates.
- **Map**: active listings plotted from their GPS coordinates.

The SQL of each saved question is also kept in the repo under `docs/metabase/*.sql`, so the dashboards can be rebuilt if the Metabase volume is lost.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A scrape-run dashboard exists (latest runs, duration, counts, failed/ABORTED runs)
- [ ] #2 A price dashboard exists showing median CZK/m² by city/citypart and disposition, with sample size
- [ ] #3 A dashboard exists showing new listings and price drops since the last run
- [ ] #4 A data-quality dashboard exists (null rates per `listing_detail` column, energy class distribution, suspected duplicates)
- [ ] #5 A map of active listings from GPS exists
- [ ] #6 The SQL of every saved question is stored in the repo under `docs/metabase/*.sql`
<!-- AC:END -->
