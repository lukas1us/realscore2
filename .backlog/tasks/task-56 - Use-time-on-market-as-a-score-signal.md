---
id: TASK-56
title: Use time on market as a score signal
status: To Do
assignee: []
created_date: '2026-11-06T13:55:00Z'
labels: []
dependencies: []
priority: low
type: feature
ordinal: 69000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A flat that has been online for months, and perhaps already cut its price, is a better negotiation target than a fresh one, but the score ignores time on market. README says days on market count from the first time realscore saw a listing because the detail has no publication date. However, the v1 payload has `since` (e.g. "2026-09-09") and `edited` dates (fixture `detail_success.json`), which may be the publication and last-edit dates. That would also fix the count for listings that were online before the first scrape.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The meaning of since and edited is checked against real listings and recorded in the Backlog doc "Sreality API"
- [ ] #2 Days on market use the publication date when it is known, and the first-seen date otherwise
- [ ] #3 The score response shows days on market, and a configurable score part or factor uses it
- [ ] #4 Tests cover a fresh listing, a long-listed one, and one without a publication date
- [ ] #5 README.md documents how days on market are counted and scored
<!-- AC:END -->
