---
id: TASK-25
title: 'Later: UI'
status: To Do
assignee: []
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-11-02T19:59:00Z'
labels:
  - ui
dependencies:
  - TASK-36
  - TASK-38
  - TASK-39
  - TASK-40
  - TASK-41
  - TASK-42
ordinal: 25000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
UI for the collected data, replacing the old app's Streamlit frontend. Done in two steps, because we don't yet know which views are worth building:

1. **Metabase first** (TASK-25.1, TASK-25.2): Metabase runs in docker compose, connected directly to Postgres as a read-only role, for exploring the data while it accumulates. Dashboards are built by hand in the Metabase UI; their SQL is kept in the repo.
2. **Custom UI later** (TASK-25.3): a `web` module (Thymeleaf + htmx) scoped by the Metabase views that actually proved useful after a couple of weeks of use. TASK-25.3 only writes that scope down; building the module will be split into further tasks then.

Metabase is a read-only exploration tool outside the application's module structure (see the decision "Metabase for data exploration; custom UI only for proven workflows").
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Subtasks TASK-25.1 (Metabase setup), TASK-25.2 (exploration dashboards) and TASK-25.3 (custom UI scope) are done
<!-- AC:END -->
