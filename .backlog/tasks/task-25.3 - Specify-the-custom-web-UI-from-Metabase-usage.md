---
id: TASK-25.3
title: Specify the custom web UI from Metabase usage
status: To Do
assignee: []
created_date: '2026-11-02T19:59:00Z'
labels:
  - ui
dependencies:
  - TASK-25.2
parent_task_id: TASK-25
ordinal: 55000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
After about two weeks of using the Metabase dashboards from TASK-25.2, write down which views were actually used and turn them into the scope for the custom web UI: a `web` module built with Thymeleaf + htmx. Building only what proved useful avoids a UI full of views nobody opens.

Keep it coarse: this task produces the scope, not the implementation, and is not split further yet. Implementation tasks are created from the result.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The Metabase views actually used during ~2 weeks are written down
- [ ] #2 Those views are turned into a coarse scope for the Thymeleaf + htmx `web` module, recorded in this task
<!-- AC:END -->
